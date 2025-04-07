package org.ruoyi.knowledgegraph.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.neo4j")
public class Neo4jProperties {
    private String uri;
    private Authentication authentication;

    @Data
    public static class Authentication {
        private String username;
        private String password;
    }

    public String getUsername() {
        return authentication != null ? authentication.getUsername() : null;
    }

    public String getPassword() {
        return authentication != null ? authentication.getPassword() : null;
    }
}