package org.ruoyi.knowledgegraph.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Utility to initialize the database with test data
 */
@Component
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Initialize the database with test data when the 'init-db' profile is active
     */
    @Bean
    @Profile("init-db")
    public CommandLineRunner initDatabase() {
        return args -> {
            logger.info("Initializing database with test data...");

            try {
                Resource resource = new ClassPathResource("sql/init.sql");
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
                populator.execute(dataSource);

                logger.info("Database initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize database: {}", e.getMessage());
            }
        };
    }
}