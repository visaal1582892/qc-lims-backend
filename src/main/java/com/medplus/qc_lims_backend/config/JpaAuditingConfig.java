package com.medplus.qc_lims_backend.config;

import com.medplus.qc_lims_backend.security.CustomUserDetails;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables JPA auditing so that @CreatedBy / @LastModifiedBy resolve to the current employee_id.
 * Rules.md §2.1, §3.4.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
