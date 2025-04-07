package org.ruoyi.knowledgegraph.connector.impl;


import org.neo4j.driver.Record;
import org.ruoyi.knowledgegraph.config.properties.Neo4jProperties;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Neo4jConnectorImpl implements org.ruoyi.knowledgegraph.connector.Neo4jConnector {
    private static final Logger logger = LoggerFactory.getLogger(Neo4jConnectorImpl.class);

    private final Neo4jProperties properties;
    private Driver driver;

    public Neo4jConnectorImpl(Neo4jProperties properties) {
        this.properties = properties;
        connect();
    }

    @Override
    public boolean connect() {
        try {
            this.driver = GraphDatabase.driver(
                    properties.getUri(),
                    AuthTokens.basic(properties.getUsername(), properties.getPassword())
            );
            // Test the connection
            try (Session session = driver.session()) {
                session.run("RETURN 1").consume();
            }
            logger.info("Successfully connected to Neo4j database at {}", properties.getUri());
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to Neo4j database: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String query, Map<String, Object> params) {
        try (Session session = driver.session()) {
            Result result = session.run(query, params != null ? params : Collections.emptyMap());
            return result.list(record -> {
                Map<String, Object> map = new HashMap<>();
                record.keys().forEach(key -> map.put(key, record.get(key).asObject()));
                return map;
            });
        } catch (Exception e) {
            logger.error("Failed to execute Neo4j query: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> executeSingleQuery(String query, Map<String, Object> params) {
        try (Session session = driver.session()) {
            Result result = session.run(query, params != null ? params : Collections.emptyMap());
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> map = new HashMap<>();
                record.keys().forEach(key -> map.put(key, record.get(key).asObject()));
                return map;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Failed to execute Neo4j single query: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public int executeUpdate(String statement, Map<String, Object> params) {
        try (Session session = driver.session()) {
            Result result = session.run(statement, params != null ? params : Collections.emptyMap());
            return result.consume().counters().nodesCreated() +
                    result.consume().counters().nodesDeleted() +
                    result.consume().counters().relationshipsCreated() +
                    result.consume().counters().relationshipsDeleted();
        } catch (Exception e) {
            logger.error("Failed to execute Neo4j update: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean createConstraint(String label, String property) {
        try (Session session = driver.session()) {
            try {
                // Try modern syntax first
                session.run(
                        "CREATE CONSTRAINT IF NOT EXISTS FOR (n:" + label + ") REQUIRE n." + property + " IS UNIQUE"
                ).consume();
            } catch (Exception e) {
                // Fall back to legacy syntax
                session.run(
                        "CREATE CONSTRAINT ON (n:" + label + ") ASSERT n." + property + " IS UNIQUE"
                ).consume();
            }
            logger.info("Successfully created constraint for {}:{}", label, property);
            return true;
        } catch (Exception e) {
            logger.error("Failed to create Neo4j constraint: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createNodeKeyConstraint(String label, String... properties) {
        try (Session session = driver.session()) {
            String propertiesStr = Arrays.stream(properties)
                    .map(p -> "n." + p)
                    .collect(Collectors.joining(", "));

            try {
                // Try modern syntax first
                session.run(
                        "CREATE CONSTRAINT IF NOT EXISTS FOR (n:" + label + ") REQUIRE (" + propertiesStr + ") IS NODE KEY"
                ).consume();
            } catch (Exception e) {
                // Fall back to legacy syntax
                session.run(
                        "CREATE CONSTRAINT ON (n:" + label + ") ASSERT (" + propertiesStr + ") IS NODE KEY"
                ).consume();
            }
            logger.info("Successfully created node key constraint for {}:{}", label, String.join(", ", properties));
            return true;
        } catch (Exception e) {
            logger.error("Failed to create Neo4j node key constraint: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean clearDatabase() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
            logger.info("Successfully cleared Neo4j database");
            return true;
        } catch (Exception e) {
            logger.error("Failed to clear Neo4j database: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean constraintExists(String constraintName) {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "SHOW CONSTRAINTS WHERE name = $name",
                    Collections.singletonMap("name", constraintName)
            );
            return result.hasNext();
        } catch (Exception e) {
            logger.error("Failed to check if Neo4j constraint exists: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean dropConstraint(String constraintName) {
        try (Session session = driver.session()) {
            session.run("DROP CONSTRAINT " + constraintName).consume();
            logger.info("Successfully dropped Neo4j constraint: {}", constraintName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to drop Neo4j constraint: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listConstraints() {
        try (Session session = driver.session()) {
            Result result = session.run("SHOW CONSTRAINTS");
            return result.list(record -> record.get("name").asString());
        } catch (Exception e) {
            logger.error("Failed to list Neo4j constraints: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
            logger.info("Neo4j database connection closed");
        }
    }

    @Override
    public String getName() {
        return "Neo4j";
    }
}