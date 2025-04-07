package org.ruoyi.knowledgegraph.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "knowledge-graph")
public class KnowledgeGraphProperties {
    private Initialization initialization = new Initialization();
    private Connector connector = new Connector();
    private Verification verification = new Verification();

    @Data
    public static class Initialization {
        private boolean createConstraints = true;
        private boolean clearDatabaseOnStartup = false;
    }

    @Data
    public static class Connector {
        private DatabaseProperties mysql = new DatabaseProperties();
        private DatabaseProperties neo4j = new DatabaseProperties();
    }

    @Data
    public static class DatabaseProperties {
        private boolean enabled = true;
    }

    @Data
    public static class Verification {
        private boolean enabled = true;
        private boolean verifyPaths = true;
    }
}