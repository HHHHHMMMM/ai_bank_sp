package org.ruoyi.knowledgegraph.service.impl;


import org.ruoyi.knowledgegraph.config.properties.KnowledgeGraphProperties;
import org.ruoyi.knowledgegraph.connector.Neo4jConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to manage Neo4j database constraints
 */
@Service
public class ConstraintService {
    private static final Logger logger = LoggerFactory.getLogger(ConstraintService.class);

    private final Neo4jConnector neo4jConnector;
    private final KnowledgeGraphProperties properties;

    @Autowired
    public ConstraintService(Neo4jConnector neo4jConnector, KnowledgeGraphProperties properties) {
        this.neo4jConnector = neo4jConnector;
        this.properties = properties;
    }

    /**
     * Create all necessary constraints in the Neo4j database
     */
    public void createConstraints() {
        if (!properties.getInitialization().isCreateConstraints()) {
            logger.info("Constraint creation disabled in configuration");
            return;
        }

        try {
            // List existing constraints first
            List<String> constraints = neo4jConnector.listConstraints();
            logger.info("Found {} existing constraints", constraints.size());

            // Clear existing constraints for Problem and Step if needed
            for (String constraintName : constraints) {
                if (constraintName.contains("Problem") || constraintName.contains("Step")) {
                    neo4jConnector.dropConstraint(constraintName);
                    logger.info("Dropped constraint: {}", constraintName);
                }
            }

            // Create new constraints
            boolean success = neo4jConnector.createConstraint("Problem", "problem_id");
            if (success) {
                logger.info("Successfully created constraint for Problem.problem_id");
            }

            success = neo4jConnector.createNodeKeyConstraint("Step", "problem_id", "step_id");
            if (success) {
                logger.info("Successfully created node key constraint for Step.problem_id and Step.step_id");
            }
        } catch (Exception e) {
            logger.warn("Couldn't create constraints: {}. Continuing without constraints.", e.getMessage());
        }
    }

    /**
     * Check if all necessary constraints exist
     * @return true if all constraints exist
     */
    public boolean checkConstraints() {
        try {
            List<String> constraints = neo4jConnector.listConstraints();

            boolean hasProblemConstraint = false;
            boolean hasStepConstraint = false;

            for (String constraintName : constraints) {
                if (constraintName.contains("Problem") && constraintName.contains("problem_id")) {
                    hasProblemConstraint = true;
                }
                if (constraintName.contains("Step") && constraintName.contains("problem_id")
                        && constraintName.contains("step_id")) {
                    hasStepConstraint = true;
                }
            }

            return hasProblemConstraint && hasStepConstraint;
        } catch (Exception e) {
            logger.error("Error checking constraints: {}", e.getMessage());
            return false;
        }
    }
}
