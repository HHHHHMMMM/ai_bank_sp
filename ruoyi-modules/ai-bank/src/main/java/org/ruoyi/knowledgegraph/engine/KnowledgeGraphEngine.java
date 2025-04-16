package org.ruoyi.knowledgegraph.engine;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class KnowledgeGraphEngine {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphEngine.class);

    private final Driver neo4jDriver;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired
    public KnowledgeGraphEngine(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    public void close() {
        if (neo4jDriver != null) {
            neo4jDriver.close();
        }
    }

    /**
     * 根据意图查找对应的问题ID
     *
     * @param intent 意图类型
     * @return 问题ID或null
     */
    public String findProblemByIntent(String intent) {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(
                    "MATCH (p:Problem {problem_type: $problem_type}) " +
                            "RETURN p.problem_id AS problem_id",
                    Values.parameters("problem_type", intent)
            );

            if (result.hasNext()) {
                Record record = result.next();
                return record.get("problem_id").asString();
            }
            return null;
        } catch (Exception e) {
            logger.error("查找问题ID失败", e);
            return null;
        }
    }

    /**
     * 执行知识图谱解决方案
     *
     * @param problemId 问题ID
     * @param context   上下文参数，包含实体值
     * @return 返回解决方案的回复内容和更新后的上下文
     */
    public SimpleEntry<String, Map<String, Object>> executeSolution(String problemId, Map<String, Object> context) {
        try (Session session = neo4jDriver.session()) {
            // 1. 找到问题的第一个步骤
            Result firstStepResult = session.run(
                    "MATCH (p:Problem {problem_id: $problem_id})-[:FIRST_STEP]->(s:Step) " +
                            "RETURN s.step_id AS step_id, s.operation AS operation, " +
                            "s.system_a AS system, s.table_name AS table_name, " +
                            "s.field AS field, s.condition_sql AS condition_sql, " +
                            "s.reply_content AS reply_content",
                    Values.parameters("problem_id", problemId)
            );

            if (!firstStepResult.hasNext()) {
                return new SimpleEntry<>("抱歉，找不到解决方案。", context);
            }

            // 2. 执行步骤链
            Record firstStepRecord = firstStepResult.next();
            Map<String, Object> currentStep = new HashMap<>();
            currentStep.put("step_id", firstStepRecord.get("step_id"));
            currentStep.put("operation", firstStepRecord.get("operation").asString());
            currentStep.put("system", firstStepRecord.get("system").asString(null));
            currentStep.put("table_name", firstStepRecord.get("table_name").asString(null));
            currentStep.put("field", firstStepRecord.get("field").asString(null));
            currentStep.put("condition_sql", firstStepRecord.get("condition_sql").asString(null));
            currentStep.put("reply_content", firstStepRecord.get("reply_content").asString(null));

            Map<String, Object> resultValues = new HashMap<>();

            while (currentStep != null) {
                // 处理当前步骤
                String operation = (String) currentStep.get("operation");
                if ("query".equals(operation)) {
                    // 执行查询操作
                    Object fieldValue = executeQuery(
                            (String) currentStep.get("system"),
                            (String) currentStep.get("table_name"),
                            (String) currentStep.get("field"),
                            (String) currentStep.get("condition_sql"),
                            context
                    );

                    // 存储查询结果到上下文
                    if (fieldValue != null) {
                        String fieldName = (String) currentStep.get("field");
                        resultValues.put(fieldName, fieldValue);
                        context.put(fieldName, fieldValue);
                        logger.info("查询结果: {} = {}", fieldName, fieldValue);
                    }
                } else if ("reply".equals(operation)) {
                    // 生成回复内容
                    String replyTemplate = (String) currentStep.get("reply_content");
                    if (replyTemplate != null && !replyTemplate.isEmpty()) {
                        // 填充模板中的变量
                        try {
                            Map<String, Object> mergedContext = new HashMap<>(context);
                            mergedContext.putAll(resultValues);
                            String reply = formatTemplate(replyTemplate, mergedContext);
                            logger.info("生成回复: {}", reply);
                            return new SimpleEntry<>(reply, mergedContext);
                        } catch (Exception e) {
                            String errorMsg = "回复生成失败，缺少参数: " + e.getMessage();
                            logger.error(errorMsg, e);
                            return new SimpleEntry<>(errorMsg, context);
                        }
                    } else {
                        String errorMsg = "抱歉，无法生成回复。";
                        logger.error(errorMsg);
                        return new SimpleEntry<>(errorMsg, context);
                    }
                }

                // 查找下一步
                currentStep = findNextStep(problemId, String.valueOf(currentStep.get("step_id")), context, resultValues, session);
                if (currentStep == null) {
                    return new SimpleEntry<>("抱歉，处理过程中断。", context);
                }
            }

            return new SimpleEntry<>("抱歉，处理过程异常终止。", context);
        } catch (Exception e) {
            logger.error("执行解决方案失败", e);
            return new SimpleEntry<>("抱歉，系统处理出错。", context);
        }
    }

    /**
     * 查询系统
     */
    private Object querySystem(String tableName, String field, String condition, String system) {
        try {
            if (dataSource == null) {
                logger.error("数据源未配置");
                return null;
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 构建查询SQL
                String sql = String.format("SELECT %s FROM %s %s", field, tableName, condition);
                logger.info("执行SQL: {}", sql);

                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        return rs.getObject(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("系统查询失败", e);
        }
        return null;
    }

    /**
     * 执行系统查询
     */
    private Object executeQuery(String system, String tableName, String field, String conditionSql, Map<String, Object> context) {
        logger.info("查询系统: {}, 表: {}, 字段: {}", system, tableName, field);

        // 格式化SQL条件
        if (conditionSql != null && !conditionSql.isEmpty()) {
            try {
                String formattedCondition = formatTemplate(conditionSql, context);
                logger.info("条件: {}", formattedCondition);
                return querySystem(tableName, field, formattedCondition, system);
            } catch (Exception e) {
                logger.error("条件格式化失败，缺少参数: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * 格式化模板字符串
     */
    private String formatTemplate(String template, Map<String, Object> values) {
        if (template == null) {
            return "";
        }

        // 正则表达式匹配 {variable} 格式的变量
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = values.get(key);
            if (value == null) {
                throw new IllegalArgumentException("找不到变量: " + key);
            }
            matcher.appendReplacement(result, value.toString());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 查找下一步骤
     */
    private Map<String, Object> findNextStep(String problemId, String currentStepId,
                                             Map<String, Object> context,
                                             Map<String, Object> resultValues,
                                             Session session) {
        // 合并上下文和查询结果
        Map<String, Object> mergedContext = new HashMap<>(context);
        mergedContext.putAll(resultValues);

        // 查找所有可能的下一步
        Result nextStepsResult = session.run(
                "MATCH (s1:Step {problem_id: $problem_id, step_id: $step_id})-[r]->(s2:Step) " +
                        "RETURN r.condition AS condition, s2.step_id AS step_id, s2.operation AS operation, " +
                        "s2.system_a AS system, s2.table_name AS table_name, " +
                        "s2.field AS field, s2.condition_sql AS condition_sql, " +
                        "s2.reply_content AS reply_content, type(r) AS relation_type",
                Values.parameters(
                        "problem_id", problemId,
                        "step_id", currentStepId
                )
        );

        List<Map<String, Object>> nextSteps = new ArrayList<>();
        while (nextStepsResult.hasNext()) {
            Record record = nextStepsResult.next();
            Map<String, Object> step = new HashMap<>();
            step.put("condition", record.get("condition").asString(null));
            step.put("step_id", record.get("step_id").asString());
            step.put("operation", record.get("operation").asString());
            step.put("system", record.get("system").asString(null));
            step.put("table_name", record.get("table_name").asString(null));
            step.put("field", record.get("field").asString(null));
            step.put("condition_sql", record.get("condition_sql").asString(null));
            step.put("reply_content", record.get("reply_content").asString(null));
            step.put("relation_type", record.get("relation_type").asString());
            nextSteps.add(step);
        }

        if (nextSteps.isEmpty()) {
            logger.error("没有找到步骤 {} 的后续步骤", currentStepId);
            return null;
        }

        // 判断条件
        for (Map<String, Object> step : nextSteps) {
            String relationType = (String) step.get("relation_type");

            if ("NEXT_DEFAULT".equals(relationType)) {
                // 默认转移
                logger.info("默认转移到步骤 {}", step.get("step_id"));
                return step;
            } else if ("NEXT_IF".equals(relationType) && step.get("condition") != null) {
                // 条件转移
                String condition = (String) step.get("condition");
                if (evaluateCondition(condition, mergedContext)) {
                    logger.info("条件 '{}' 满足，转移到步骤 {}", condition, step.get("step_id"));
                    return step;
                }
            }
        }

        // 没有符合条件的下一步
        logger.error("没有符合条件的下一步");
        return null;
    }

    /**
     * 评估条件表达式（简化版）
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }

        // 替换变量
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                condition = condition.replace(key, "'" + value + "'");
            } else {
                condition = condition.replace(key, String.valueOf(value));
            }
        }

        // 处理IS NULL和IS NOT NULL
        condition = condition.replace("IS NULL", "== null");
        condition = condition.replace("IS NOT NULL", "!= null");
        condition = condition.replace("where", "");

        logger.info("评估条件: {}", condition);

        // 简化版条件评估 - 仅支持基本比较
        // 在实际项目中，应该使用更安全和全面的条件解析方法
        try {
            // 非常简单的条件判断，仅作示例
            // 实际项目中应使用专业的表达式解析库，如JEXL, MVEL等
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                return parts[0].trim().equals(parts[1].trim());
            } else if (condition.contains("!=")) {
                String[] parts = condition.split("!=");
                return !parts[0].trim().equals(parts[1].trim());
            } else if (condition.contains(">")) {
                String[] parts = condition.split(">");
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left > right;
            } else if (condition.contains("<")) {
                String[] parts = condition.split("<");
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left < right;
            }

            return false;
        } catch (Exception e) {
            logger.error("条件评估失败: {}, 错误: {}", condition, e.getMessage());
            return false;
        }
    }
}