package com.medplus.qc_lims_backend.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that supports repairing schema history before migration.
 * Enabled via {@code spring.flyway.repair-at-start=true} in application.properties.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.repair-at-start:false}")
    private boolean repairAtStart;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return (Flyway flyway) -> {
            if (repairAtStart) {
                log.info("Running Flyway repair before migration (spring.flyway.repair-at-start=true)");
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
