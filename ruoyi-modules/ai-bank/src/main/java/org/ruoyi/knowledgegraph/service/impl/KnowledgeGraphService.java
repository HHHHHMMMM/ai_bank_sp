package org.ruoyi.knowledgegraph.service.impl;


import org.ruoyi.knowledgegraph.connector.Neo4jConnector;
import org.ruoyi.knowledgegraph.domain.Problem;
import org.ruoyi.knowledgegraph.domain.Step;
import org.ruoyi.knowledgegraph.domain.StepRelation;
import org.ruoyi.knowledgegraph.repository.ProblemRepository;
import org.ruoyi.knowledgegraph.repository.StepRelationRepository;
import org.ruoyi.knowledgegraph.repository.StepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeGraphService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final Neo4jConnector neo4jConnector;
    private final ProblemRepository problemRepository;
    private final StepRepository stepRepository;
    private final StepRelationRepository stepRelationRepository;
    private final ConstraintService constraintService;

    @Autowired
    public KnowledgeGraphService(
            Neo4jConnector neo4jConnector,
            ProblemRepository problemRepository,
            StepRepository stepRepository,
            StepRelationRepository stepRelationRepository,
            ConstraintService constraintService) {
        this.neo4jConnector = neo4jConnector;
        this.problemRepository = problemRepository;
        this.stepRepository = stepRepository;
        this.stepRelationRepository = stepRelationRepository;
        this.constraintService = constraintService;
    }

    /**
     * Create the knowledge graph from the database
     * @return true if successful
     */
    public boolean createKnowledgeGraph() {
        // 1. Create constraints if needed
       // constraintService.createConstraints();

        // 2. Fetch all active problems
        List<Problem> problems = problemRepository.findAllActiveProblems();
        if (problems.isEmpty()) {
            logger.warn("No active problems found");
            return false;
        }

        logger.info("Found {} active problems", problems.size());

        // 3. For each problem, create the knowledge graph
        for (Problem problem : problems) {
            createProblemGraph(problem);
        }

        return true;
    }

    /**
     * Create the knowledge graph for a specific problem
     * @param problem the problem to create the graph for
     */
    public boolean createProblemGraph(Problem problem) {
        String problemType = problem.getProblemType();

        // Generate problem ID based on problem type (format: TYPE001, TYPE002, etc.)
        String generatedProblemId = generateProblemId(problemType);
        problem.setProblemId(generatedProblemId);

        logger.info("Processing problem: {} - {}", generatedProblemId, problemType);

        // Create problem node
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", generatedProblemId);
        params.put("problem_type", problemType);
        params.put("description", problem.getDescription());

        String query = "MERGE (p:Problem {problem_id: $problem_id, problem_type: $problem_type, description: $description})";
        int i = neo4jConnector.executeUpdate(query, params);
        return i > 0;
    }

    /**
     * Generate a unique problem ID based on problem type
     * Format: TYPE001, TYPE002, etc.
     */
    private String generateProblemId(String problemType) {
        // Query to find the maximum ID for the given problem type
        String query = "MATCH (p:Problem) WHERE p.problem_type = $problem_type " +
                "RETURN p.problem_id AS problemId ORDER BY p.problem_id DESC LIMIT 1";

        Map<String, Object> params = new HashMap<>();
        params.put("problem_type", problemType);

        Map<String, Object> result = neo4jConnector.executeSingleQuery(query, params);

        int nextNum = 1; // Default start with 001

        if (result != null && result.get("problemId") != null) {
            String latestId = (String) result.get("problemId");
            String[] parts = latestId.split("-");
            String numericPart = parts[parts.length - 1];  // 显式获取最后一个元素

            try {
                nextNum = Integer.parseInt(numericPart) + 1;
            } catch (NumberFormatException e) {
                logger.warn("Could not parse numeric part of problem ID: {}", latestId);
                // Default to 1 if parsing fails
            }
        }

        // Format: TYPE001, TYPE002, etc.
        return problemType +"-"+ String.format("%03d", nextNum);
    }

    /**
     * Update an existing problem node
     */
    public boolean updateProblemGraph(Problem problem) {
        String problemId = problem.getProblemId();
        String problemType = problem.getProblemType();

        logger.info("Updating problem: {} - {}", problemId, problemType);

        // Update problem node
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", problemId);
        params.put("problem_type", problemType);
        params.put("description", problem.getDescription());

        String query = "MATCH (p:Problem {problem_id: $problem_id}) " +
                "SET p.problem_type = $problem_type, p.description = $description";

        int i = neo4jConnector.executeUpdate(query, params);
        return i > 0;
    }
    /**
     * Create a step node in the graph
     * @param step the step to create
     */
    public  boolean createStepNode(Step step) {
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", step.getProblemId());
        params.put("step_id", step.getStepId());
        params.put("operation", step.getOperation());

        // Add additional properties
        Map<String, Object> properties = new HashMap<>();
        if (step.getSystemA() != null) properties.put("system_a", step.getSystemA());
        if (step.getTableName() != null) properties.put("table_name", step.getTableName());
        if (step.getField() != null) properties.put("field", step.getField());
        if (step.getConditionSql() != null) properties.put("condition_sql", step.getConditionSql());
        if (step.getReplyContent() != null) properties.put("reply_content", step.getReplyContent());

        params.put("properties", properties);

        String query = "MERGE (s:Step {problem_id: $problem_id, step_id: $step_id, operation: $operation}) " +
                "SET s += $properties";
        int i = neo4jConnector.executeUpdate(query, params);
        return i > 0;
    }

    /**
     * Create a relation between steps
     * @param relation the relation to create
     */
    public boolean createStepRelation(StepRelation relation) {
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", relation.getProblemId());

        // 首先检查问题节点是否存在
        String checkProblemQuery = "MATCH (p:Problem {problem_id: $problem_id}) RETURN count(p) as count";
        List<Map<String, Object>> problemResult = neo4jConnector.executeQuery(checkProblemQuery, params);

        if (problemResult == null || problemResult.isEmpty() || (Long)problemResult.get(0).get("count") == 0) {
            System.out.println("找不到问题节点，问题ID: " + relation.getProblemId());
            return false;
        }

        // FIRST_STEP关系
        if ("FIRST_STEP".equals(relation.getRelationType())) {
            // 检查目标步骤节点是否存在
            params.put("to_id", relation.getToStepId());
            String checkStepQuery = "MATCH (s:Step) WHERE s.problem_id = $problem_id AND s.step_id = $to_id RETURN count(s) as count";
            List<Map<String, Object>> stepResult = neo4jConnector.executeQuery(checkStepQuery, params);

            if (stepResult == null || stepResult.isEmpty() || (Long)stepResult.get(0).get("count") == 0) {
                System.out.println("找不到步骤节点，问题ID: " + relation.getProblemId() + ", 步骤ID: " + relation.getToStepId());
                return false;
            }

            String query = "MATCH (p:Problem {problem_id: $problem_id}) " +
                    "MATCH (s:Step {problem_id: $problem_id, step_id: $to_id}) " +
                    "CREATE (p)-[:FIRST_STEP]->(s)";

            int i = neo4jConnector.executeUpdate(query, params);
            return i > 0;
        }
        // 其他关系类型
        else {
            params.put("from_id", relation.getFromStepId());
            params.put("to_id", relation.getToStepId());

            // 检查源步骤节点是否存在
            String checkFromStepQuery = "MATCH (s:Step {problem_id: $problem_id, step_id: $from_id}) RETURN count(s) as count";
            List<Map<String, Object>> fromStepResult = neo4jConnector.executeQuery(checkFromStepQuery, params);

            if (fromStepResult == null || fromStepResult.isEmpty() || (Long)fromStepResult.get(0).get("count") == 0) {
                System.out.println("找不到源步骤节点，问题ID: " + relation.getProblemId() + ", 步骤ID: " + relation.getFromStepId());
                return false;
            }

            // 检查目标步骤节点是否存在
            String checkToStepQuery = "MATCH (s:Step {problem_id: $problem_id, step_id: $to_id}) RETURN count(s) as count";
            List<Map<String, Object>> toStepResult = neo4jConnector.executeQuery(checkToStepQuery, params);

            if (toStepResult == null || toStepResult.isEmpty() || (Long)toStepResult.get(0).get("count") == 0) {
                System.out.println("找不到目标步骤节点，问题ID: " + relation.getProblemId() + ", 步骤ID: " + relation.getToStepId());
                return false;
            }

            String query;
            if ("NEXT_DEFAULT".equals(relation.getRelationType())) {
                query = "MATCH (s1:Step {problem_id: $problem_id, step_id: $from_id}) " +
                        "MATCH (s2:Step {problem_id: $problem_id, step_id: $to_id}) " +
                        "CREATE (s1)-[:NEXT_DEFAULT]->(s2)";
            } else {
                params.put("condition", relation.getConditionExpression());
                query = "MATCH (s1:Step {problem_id: $problem_id, step_id: $from_id}) " +
                        "MATCH (s2:Step {problem_id: $problem_id, step_id: $to_id}) " +
                        "CREATE (s1)-[:NEXT_IF {condition: $condition}]->(s2)";
            }

            int i = neo4jConnector.executeUpdate(query, params);
            return i > 0;
        }
    }
    /**
     * Clear the entire knowledge graph
     * @return true if successful
     */
    public boolean clearKnowledgeGraph() {
        return neo4jConnector.clearDatabase();
    }



    /**
     * 获取图谱数据
     * @return 图谱数据
     */
    /**
     * 获取图谱数据
     * @return 图谱数据
     */
    public Map<String, Object> getGraphData() {
        Map<String, Object> result = new HashMap<>();

        // 修改节点查询，直接提取需要的属性
        String nodeQuery = "MATCH (n) RETURN id(n) as id, n.name as name, labels(n)[0] as nodeType, properties(n) as properties";
        List<Map<String, Object>> nodes = neo4jConnector.executeQuery(nodeQuery, new HashMap<>());

        // 修改关系查询
        String relationQuery = "MATCH ()-[r]->() RETURN id(r) as id, id(startNode(r)) as source, id(endNode(r)) as target, type(r) as relationLabel, properties(r) as properties";
        List<Map<String, Object>> edges = neo4jConnector.executeQuery(relationQuery, new HashMap<>());

        result.put("nodes", nodes);
        result.put("edges", edges);

        return result;
    }

    /**
     * 删除节点
     * @param nodeId 节点ID
     * @return 是否成功
     */
    public boolean deleteNode(String nodeId) {
        try {
            String query = "MATCH (n) WHERE id(n) = $nodeId DETACH DELETE n";

            Map<String, Object> params = new HashMap<>();
            // 将字符串转换为整数
            params.put("nodeId", Integer.parseInt(nodeId));

            int i = neo4jConnector.executeUpdate(query, params);
            if(i > 0) return true;
        } catch (Exception e) {
            logger.error("Failed to delete node", e);
            return false;
        }
        return false;
    }
    /**
     * 删除关系
     * @param relationId 关系ID
     * @return 是否成功
     */
    public boolean deleteRelation(String relationId) {
        try {
            String query = "MATCH ()-[r]->() WHERE id(r) = $relationId DELETE r";

            Map<String, Object> params = new HashMap<>();
            params.put("relationId", relationId);

            neo4jConnector.executeUpdate(query, params);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete relation", e);
            return false;
        }
    }


    /**
     * 获取所有问题ID列表
     * @return 问题ID列表
     */
    public List<Problem> listProblemIds() {
        String query = "MATCH (p:Problem) RETURN p.problem_id as problemId, " +
                "p.description as description, " +
                "p.problem_type as problemType";

        List<Map<String, Object>> results = neo4jConnector.executeQuery(query,new HashMap<>());
        List<Problem> problemIds = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> row : results) {
                Problem vo = new Problem();
                vo.setProblemId((String) row.get("problemId"));
                vo.setDescription((String) row.get("description"));
                vo.setProblemType((String) row.get("problemType"));
                problemIds.add(vo);
            }
        }

        return problemIds;
    }

    public List<Step> getStepsByProblemId(String problemId) {
        String query = "MATCH (s:Step {problem_id: $problemId}) " +
                "RETURN s.step_id as stepId, s.operation as operation, " +
                "id(s) as id, s.problem_id as problemId";

        Map<String, Object> params = new HashMap<>();
        params.put("problemId", problemId);

        List<Map<String, Object>> results = neo4jConnector.executeQuery(query, params);
        List<Step> steps = new ArrayList<>();

        if (results != null && !results.isEmpty()) {
            for (Map<String, Object> row : results) {
                Step step = new Step();
                step.setId(Long.valueOf(row.get("id").toString()));
                step.setProblemId((String) row.get("problemId"));
                step.setStepId((Long) row.get("stepId"));
                step.setOperation((String) row.get("operation"));
                steps.add(step);
            }
        }

        return steps;
    }
}