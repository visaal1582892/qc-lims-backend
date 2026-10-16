package com.medplus.qc_lims_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA auditing configuration.
 *
 * Required for BaseEntity @EntityListeners(AuditingEntityListener.class)
 * to populate createdAt, updatedAt automatically.
 *
 * Review correction #2: Added as a separate config class for clarity.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
