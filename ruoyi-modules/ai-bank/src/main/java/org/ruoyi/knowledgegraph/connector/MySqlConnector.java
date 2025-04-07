package org.ruoyi.knowledgegraph.connector;


import org.ruoyi.knowledgegraph.connector.DatabaseConnector;
import org.ruoyi.knowledgegraph.config.properties.MySqlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MySqlConnector implements DatabaseConnector {
    private static final Logger logger = LoggerFactory.getLogger(MySqlConnector.class);

    private final DataSource dataSource;
    private final MySqlProperties properties;
    private NamedParameterJdbcTemplate jdbcTemplate;

    public MySqlConnector(DataSource dataSource, MySqlProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
        connect();
    }

    @Override
    public boolean connect() {
        try {
            this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            logger.info("Successfully connected to MySQL database {}", properties.getDatabase());
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to MySQL database: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String query, Map<String, Object> params) {
        try {
            return jdbcTemplate.queryForList(query, params != null ? params : Collections.emptyMap());
        } catch (Exception e) {
            logger.error("Failed to execute query: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> executeSingleQuery(String query, Map<String, Object> params) {
        try {
            return jdbcTemplate.queryForMap(query, params != null ? params : Collections.emptyMap());
        } catch (Exception e) {
            logger.error("Failed to execute single query: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public int executeUpdate(String statement, Map<String, Object> params) {
        try {
            return jdbcTemplate.update(statement, params != null ? params : Collections.emptyMap());
        } catch (Exception e) {
            logger.error("Failed to execute update: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public void close() {
        // Spring manages the connection pool, no need to explicitly close
        logger.info("MySQL database connection management handled by Spring");
    }

    @Override
    public String getName() {
        return "MySQL";
    }
}

