package org.ruoyi.knowledgegraph.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class KnowledgeGraphConfig {

    @Value("${knowledge.graph.neo4j.uri:bolt://localhost:7687}")
    private String neo4jUri;

    @Value("${knowledge.graph.neo4j.username:neo4j}")
    private String neo4jUsername;

    @Value("${knowledge.graph.neo4j.password:neo4j123}")
    private String neo4jPassword;

    @Value("${knowledge-graph.enabled:false}")
    private boolean knowledgeGraphEnabled;

    public boolean isKnowledgeGraphEnabled() {
        return knowledgeGraphEnabled;
    }
}