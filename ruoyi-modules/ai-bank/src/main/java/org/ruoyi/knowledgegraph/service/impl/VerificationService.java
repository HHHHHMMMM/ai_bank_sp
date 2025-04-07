package org.ruoyi.knowledgegraph.service.impl;


import org.ruoyi.knowledgegraph.config.properties.KnowledgeGraphProperties;
import org.ruoyi.knowledgegraph.connector.Neo4jConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to verify the knowledge graph
 */
@Service
public class VerificationService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private final Neo4jConnector neo4jConnector;
    private final KnowledgeGraphProperties properties;

    @Autowired
    public VerificationService(Neo4jConnector neo4jConnector, KnowledgeGraphProperties properties) {
        this.neo4jConnector = neo4jConnector;
        this.properties = properties;
    }

    /**
     * Verify the entire knowledge graph
     * @return true if the verification is successful
     */
    public boolean verifyKnowledgeGraph() {
        if (!properties.getVerification().isEnabled()) {
            logger.info("Knowledge graph verification disabled in configuration");
            return true;
        }

        boolean success = true;

        if (properties.getVerification().isVerifyPaths()) {
            success = verifyPaths();
        }

        return success;
    }

    /**
     * Verify that all problems have valid paths
     * @return true if all paths are valid
     */
    private boolean verifyPaths() {
        boolean allValid = true;

        // Find all problems
        String query = "MATCH (p:Problem) RETURN p.problem_id AS problem_id, p.problem_type AS problem_type";
        List<Map<String, Object>> problems = neo4jConnector.executeQuery(query, Collections.emptyMap());

        for (Map<String, Object> problem : problems) {
            String problemId = (String) problem.get("problem_id");
            String problemType = (String) problem.get("problem_type");

            // Check if the problem has a first step
            Map<String, Object> params = new HashMap<>();
            params.put("problem_id", problemId);

            query = "MATCH (p:Problem {problem_id: $problem_id})-[:FIRST_STEP]->(s:Step) RETURN s.step_id AS step_id";
            Map<String, Object> firstStep = neo4jConnector.executeSingleQuery(query, params);

            if (firstStep.isEmpty()) {
                logger.error("Problem {} has no first step!", problemId);
                allValid = false;
                continue;
            }

            // Check if there are end steps (steps with no outgoing relationships)
            query = "MATCH (p:Problem {problem_id: $problem_id})-[:FIRST_STEP]->(:Step)-[:NEXT_IF|NEXT_DEFAULT*0..10]->(s:Step) " +
                    "WHERE NOT (s)-[:NEXT_IF|NEXT_DEFAULT]->() " +
                    "RETURN s.step_id AS step_id, s.operation AS operation";
            List<Map<String, Object>> endSteps = neo4jConnector.executeQuery(query, params);

            if (endSteps.isEmpty()) {
                logger.error("Problem {} has no end steps!", problemId);
                allValid = false;
            } else {
                // Verify that all end steps are reply operations
                for (Map<String, Object> step : endSteps) {
                    Long stepId = (Long) step.get("step_id");  // 改为 Long
                    String operation = (String) step.get("operation");

                    if (!"reply".equals(operation)) {
                        logger.error("End step {} of problem {} is not a reply operation!", stepId, problemId);
                        allValid = false;
                    }
                }


                logger.info("Problem {} ({}) path verification complete, found {} end steps",
                        problemId, problemType, endSteps.size());
            }
        }

        return allValid;
    }
}
