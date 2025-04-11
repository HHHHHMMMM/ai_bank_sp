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
    public  boolean createProblemGraph(Problem problem) {
        String problemId = problem.getProblemId();
        String problemType = problem.getProblemType();

        logger.info("Processing problem: {} - {}", problemId, problemType);

        // 1. Create problem node
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", problemId);
        params.put("problem_type", problemType);
        params.put("description", problem.getDescription());

        String query = "MERGE (p:Problem {problem_id: $problem_id, problem_type: $problem_type, description: $description})";
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
        params.put("from_id", relation.getFromStepId());
        params.put("to_id", relation.getToStepId());

        String query;
        if ("NEXT_DEFAULT".equals(relation.getRelationType())) {
            query = "MATCH (s1:Step {problem_id: $problem_id, step_id: $from_id}) " +
                    "MATCH (s2:Step {problem_id: $problem_id, step_id: $to_id}) " +
                    "MERGE (s1)-[:NEXT_DEFAULT]->(s2)";
        } else {
            params.put("condition", relation.getConditionExpression());
            query = "MATCH (s1:Step {problem_id: $problem_id, step_id: $from_id}) " +
                    "MATCH (s2:Step {problem_id: $problem_id, step_id: $to_id}) " +
                    "MERGE (s1)-[:NEXT_IF {condition: $condition}]->(s2)";
        }

        int i = neo4jConnector.executeUpdate(query, params);
        return i > 0;
    }

    /**
     * Clear the entire knowledge graph
     * @return true if successful
     */
    public boolean clearKnowledgeGraph() {
        return neo4jConnector.clearDatabase();
    }

//    /**
//     * 创建节点
//     * @param nodeData 节点数据
//     * @return 是否成功
//     */
//    public boolean createNode(Map<String, Object> nodeData) {
//        try {
//            String label = (String) nodeData.getOrDefault("label", "Node");
//            String query = "CREATE (n:" + label + ") SET n = $properties RETURN id(n)";
//
//            Map<String, Object> params = new HashMap<>();
//            params.put("properties", nodeData);
//
//            neo4jConnector.executeUpdate(query, params);
//            return true;
//        } catch (Exception e) {
//            logger.error("Failed to create node", e);
//            return false;
//        }
//    }

//    /**
//     * 创建关系
//     * @param relationData 关系数据
//     * @return 是否成功
//     */
//    public boolean createRelation(Map<String, Object> relationData) {
//        try {
//            String sourceId = (String) relationData.get("sourceId");
//            String targetId = (String) relationData.get("targetId");
//            String type = (String) relationData.get("type");
//            Map<String, Object> properties = (Map<String, Object>) relationData.getOrDefault("properties", new HashMap<>());
//
//            String query = "MATCH (a), (b) WHERE id(a) = $sourceId AND id(b) = $targetId " +
//                    "CREATE (a)-[r:" + type + "]->(b) SET r = $properties RETURN id(r)";
//
//            Map<String, Object> params = new HashMap<>();
//            params.put("sourceId", sourceId);
//            params.put("targetId", targetId);
//            params.put("properties", properties);
//
//            neo4jConnector.executeUpdate(query, params);
//            return true;
//        } catch (Exception e) {
//            logger.error("Failed to create relation", e);
//            return false;
//        }
//    }

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
            params.put("nodeId", nodeId);

            neo4jConnector.executeUpdate(query, params);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete node", e);
            return false;
        }
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
}