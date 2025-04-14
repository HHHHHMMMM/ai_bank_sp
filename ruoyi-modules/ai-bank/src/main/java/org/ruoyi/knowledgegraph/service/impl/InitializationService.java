package org.ruoyi.knowledgegraph.service.impl;


import org.ruoyi.knowledgegraph.config.properties.KnowledgeGraphProperties;
import org.ruoyi.knowledgegraph.connector.Neo4jConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service to handle application initialization
 */
@Service
public class InitializationService {
    private static final Logger logger = LoggerFactory.getLogger(InitializationService.class);

    private final KnowledgeGraphProperties properties;
    private final Neo4jConnector neo4jConnector;
    private final KnowledgeGraphService knowledgeGraphService;
    private final VerificationService verificationService;

    @Autowired
    public InitializationService(
            KnowledgeGraphProperties properties,
            Neo4jConnector neo4jConnector,
            KnowledgeGraphService knowledgeGraphService,
            VerificationService verificationService) {
        this.properties = properties;
        this.neo4jConnector = neo4jConnector;
        this.knowledgeGraphService = knowledgeGraphService;
        this.verificationService = verificationService;
    }

    /**
     * Initialize the application when it's ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        logger.info("Initializing Knowledge Graph Creator...");

        // Clear database if configured
        if (properties.getInitialization().isClearDatabaseOnStartup()) {
            logger.info("Clearing Neo4j database as configured");
            neo4jConnector.clearDatabase();
        }

        // Create the knowledge graph
//        boolean success = knowledgeGraphService.createKnowledgeGraph();
//        if (success) {
//            logger.info("Successfully created knowledge graph");
//
//            // Verify the knowledge graph
//            boolean verified = verificationService.verifyKnowledgeGraph();
//            if (verified) {
//                logger.info("Knowledge graph verification successful");
//            } else {
//                logger.warn("Knowledge graph verification failed");
//            }
//        } else {
//            logger.error("Failed to create knowledge graph");
//        }

        logger.info("Knowledge Graph Creator initialization complete");
    }
}