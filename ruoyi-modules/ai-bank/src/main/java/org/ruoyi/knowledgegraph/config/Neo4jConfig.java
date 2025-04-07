package org.ruoyi.knowledgegraph.config;


import org.ruoyi.knowledgegraph.config.properties.Neo4jProperties;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j configuration
 */
@Configuration
public class Neo4jConfig {

    @Bean
    public Driver neo4jDriver(Neo4jProperties properties) {
        return GraphDatabase.driver(
                properties.getUri(),
                AuthTokens.basic(properties.getUsername(), properties.getPassword())
        );
    }
}