package com.medplus.qc_lims_backend.config;

import com.medplus.qc_lims_backend.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resolves the current auditor (employee_id) for JPA @CreatedBy / @LastModifiedBy.
 */
@Configuration
public class AuditorAwareImpl {

    @Bean("auditorAware")
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Optional.of("SYSTEM");
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails cud) {
                return Optional.ofNullable(cud.getUser().getEmployeeId());
            }
            return Optional.of("SYSTEM");
        };
    }
}
