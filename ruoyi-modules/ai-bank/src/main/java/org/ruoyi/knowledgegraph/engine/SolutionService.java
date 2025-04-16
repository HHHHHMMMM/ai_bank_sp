package org.ruoyi.knowledgegraph.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.knowledgegraph.config.KnowledgeGraphConfig;
import org.ruoyi.knowledgegraph.service.impl.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SolutionService {
    private static final Logger logger = LoggerFactory.getLogger(SolutionService.class);

    private final KnowledgeGraphEngine graphEngine;
    private final KnowledgeGraphConfig knowledgeGraphConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 用户上下文缓存
    private final Map<String, Map<String, Object>> userContexts = new ConcurrentHashMap<>();
    @Autowired
    private LLMService llmService;

    @Autowired
    public SolutionService(
            KnowledgeGraphEngine graphEngine,
            KnowledgeGraphConfig knowledgeGraphConfig) {
        this.graphEngine = graphEngine;
        this.knowledgeGraphConfig = knowledgeGraphConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理用户查询
     *
     * @param userId 用户ID
     * @param query  用户查询文本
     * @return 处理结果
     */
    public String processQuery(String userId, String query,String modelName) {
        logger.info("处理用户 {} 的查询: {}", userId, query);

        // 获取或创建用户上下文
        Map<String, Object> context = userContexts.computeIfAbsent(userId, k -> new HashMap<>());

        // 使用当前LLM进行意图识别
        Map<String, Object> intentResult = extractIntent("客户号为:" + userId + "的客户有问题为：" + query,modelName);
        logger.info("识别结果: {}", intentResult);

        String intent = (String) intentResult.getOrDefault("intent", "unknown");
        @SuppressWarnings("unchecked")
        Map<String, Object> entities = (Map<String, Object>) intentResult.getOrDefault("entities", new HashMap<>());
        double confidence = Double.parseDouble(intentResult.getOrDefault("confidence", "0.0").toString());

        // 更新上下文
        context.putAll(entities);

        // 处理未知意图
        if ("unknown".equals(intent) || confidence < 0.6) {
            return "抱歉，我不确定您的具体问题。请问您是想查询账户余额，处理转账问题，还是激活银行卡？";
        }

        // 查找问题ID
        String problemId = graphEngine.findProblemByIntent(intent);
        if (problemId == null) {
            return String.format("抱歉，我们目前无法处理 %s 类型的问题。", intent);
        }

        // 执行知识图谱解决方案
        SimpleEntry<String, Map<String, Object>> result = graphEngine.executeSolution(problemId, context);
        String response = result.getKey();
        Map<String, Object> updatedContext = result.getValue();

        // 更新用户上下文
        userContexts.put(userId, updatedContext);

        // 是否需要增强回复
        return enhanceResponse(response, query, updatedContext);
    }

    /**
     * 从用户查询中提取意图和实体
     */
    public Map<String, Object> extractIntent(String userQuery,String modelName) {
        try {
            // 构建提示词
            String systemPrompt = "你是一个专业的意图识别系统，请严格按照要求的JSON格式输出。";

            String userPrompt = String.format(
                    "分析以下用户查询，提取意图和相关实体。\n" +
                            "目前支持的意图类型有:\n" +
                            "1. transfer_limit_issue - 转账限额问题\n" +
                            "2. card_activation_problem - 卡片激活问题\n" +
                            "3. balance_inquiry - 余额查询\n\n" +
                            "请提取所有相关实体，可能包括:\n" +
                            "- requested_amount: 转账金额\n" +
                            "- card_id: 卡号\n" +
                            "- customer_id: 客户ID\n" +
                            "- channel: 渠道(如mobile, web, atm等)" +
                            "其中，客户号customer_id是必须要从问题中提取到的。\n\n" +
                            "以JSON格式输出，格式为:\n" +
                            "{\n" +
                            "    \"intent\": \"意图类型\",\n" +
                            "    \"entities\": {\n" +
                            "        \"实体名\": \"实体值\"\n" +
                            "    },\n" +
                            "    \"confidence\": 置信度(0到1之间的浮点数)\n" +
                            "}\n\n" +
                            "用户查询: %s", userQuery);

            // 设置请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("temperature", 0.3);
            params.put("topP", 0.95);
            params.put("stream", false);

            // 从配置获取意图分析使用的模型
            // 调用LLM服务
            String jsonResponse = llmService.askLLM(systemPrompt, userPrompt, modelName, params);

            // 如果响应为空，抛出异常
            if (StringUtils.isEmpty(jsonResponse)) {
                throw new RuntimeException("模型未返回有效响应");
            }

            // 提取JSON部分
            jsonResponse = extractJsonFromResponse(jsonResponse);

            // 解析JSON响应
            return objectMapper.readValue(jsonResponse, Map.class);
        } catch (Exception e) {
            logger.error("意图识别失败", e);
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("intent", "unknown");
            defaultResult.put("entities", new HashMap<>());
            defaultResult.put("confidence", 0.0);
            return defaultResult;
        }
    }

    private String extractJsonFromResponse(String response) {
        // 查找第一个 { 和最后一个 }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response; // 如果没找到JSON格式，返回原始响应
    }
    /**
     * 增强系统生成的回复
     */
    private String enhanceResponse(String systemResponse, String originalQuery, Map<String, Object> context) {
        try {
            // 构建提示词
            String prompt = String.format(
                    "你是一个专业的银行客服。请根据系统生成的回复内容，生成一个更自然、更有同理心的回复。\n" +
                            "回复应该保持原始内容的准确性，但可以增加一些适当的人情味，使语言更加流畅自然。\n\n" +
                            "用户原始问题: %s\n\n" +
                            "系统生成的回复:\n%s\n\n" +
                            "请基于以上内容生成优化后的回复:",
                    originalQuery, systemResponse);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "Pro/deepseek-ai/DeepSeek-V3");
            payload.put("messages", messages);
            payload.put("temperature", 0.7);
            payload.put("max_tokens", 512);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 这里应该使用您系统现有的API调用机制
            // 这只是一个示例实现
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // 在实际系统中，应调用真实的API
            // String response = restTemplate.postForObject(apiUrl, entity, String.class);

            // 这里简单返回原始响应，实际应该处理API返回结果
            return systemResponse;
        } catch (Exception e) {
            logger.error("增强回复失败", e);
            return systemResponse;  // 失败时返回原始回复
        }
    }

    /**
     * 清除用户上下文
     */
    public void clearContext(String userId) {
        userContexts.remove(userId);
        logger.info("已清除用户 {} 的上下文", userId);
    }
}