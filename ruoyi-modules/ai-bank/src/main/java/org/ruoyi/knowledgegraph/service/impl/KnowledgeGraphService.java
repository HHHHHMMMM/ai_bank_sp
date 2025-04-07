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
    private void createProblemGraph(Problem problem) {
        String problemId = problem.getProblemId();
        String problemType = problem.getProblemType();

        logger.info("Processing problem: {} - {}", problemId, problemType);

        // 1. Create problem node
        Map<String, Object> params = new HashMap<>();
        params.put("problem_id", problemId);
        params.put("problem_type", problemType);
        params.put("description", problem.getDescription());

        String query = "MERGE (p:Problem {problem_id: $problem_id, problem_type: $problem_type, description: $description})";
        neo4jConnector.executeUpdate(query, params);

        // 2. Fetch and create solution steps
        List<Step> steps = stepRepository.findByProblemId(problemId);
        if (steps.isEmpty()) {
            logger.warn("Problem {} has no solution steps", problemId);
            return;
        }

        logger.info("Found {} solution steps for problem {}", steps.size(), problemId);

        // Create step nodes
        for (Step step : steps) {
            createStepNode(step);
        }

        // 3. Create first step relation
        params = new HashMap<>();
        params.put("problem_id", problemId);

        query = "MATCH (p:Problem {problem_id: $problem_id}) " +
                "MATCH (s:Step {problem_id: $problem_id, step_id: 1}) " +
                "MERGE (p)-[:FIRST_STEP]->(s)";
        neo4jConnector.executeUpdate(query, params);

        // 4. Create step relations
        List<StepRelation> relations = stepRelationRepository.findByProblemId(problemId);
        if (relations.isEmpty()) {
            logger.warn("Problem {} has no step relations", problemId);
            return;
        }

        logger.info("Found {} step relations for problem {}", relations.size(), problemId);

        // Create relations
        for (StepRelation relation : relations) {
            createStepRelation(relation);
        }

        logger.info("Successfully created knowledge graph for problem {}", problemId);
    }

    /**
     * Create a step node in the graph
     * @param step the step to create
     */
    private void createStepNode(Step step) {
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
        neo4jConnector.executeUpdate(query, params);
    }

    /**
     * Create a relation between steps
     * @param relation the relation to create
     */
    private void createStepRelation(StepRelation relation) {
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

        neo4jConnector.executeUpdate(query, params);
    }

    /**
     * Clear the entire knowledge graph
     * @return true if successful
     */
    public boolean clearKnowledgeGraph() {
        return neo4jConnector.clearDatabase();
    }
}