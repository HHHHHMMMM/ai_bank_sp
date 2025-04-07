package org.ruoyi.knowledgegraph.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource.dynamic.datasource.mysql")
public class MySqlProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;

    // Extract database name from URL
    public String getDatabase() {
        if (url == null) {
            return "unknown";
        }
        int lastSlashIndex = url.lastIndexOf('/');
        int questionMarkIndex = url.indexOf('?', lastSlashIndex);
        if (questionMarkIndex > 0) {
            return url.substring(lastSlashIndex + 1, questionMarkIndex);
        } else {
            return url.substring(lastSlashIndex + 1);
        }
    }
}