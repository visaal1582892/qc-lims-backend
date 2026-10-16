package com.medplus.qc_lims_backend.config;

import com.medplus.qc_lims_backend.security.Rights;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration for Phase 0.
 *
 * Per SRS Module 15.2: in-memory admin user with all rights for initial development.
 * CSRF disabled for API-only initial development.
 *
 * Default credentials: admin / admin
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Collect all roles defined in Rights.java
        List<String> allRoles = new ArrayList<>();
        // Test Parameter Master
        allRoles.add(Rights.ROLE_TEST_PARAMETER_READ);
        allRoles.add(Rights.ROLE_TEST_PARAMETER_CREATE);
        allRoles.add(Rights.ROLE_TEST_PARAMETER_UPDATE);
        allRoles.add(Rights.ROLE_TEST_PARAMETER_DELETE);
        // Sample Inward Register
        allRoles.add(Rights.ROLE_INWARD_VIEW);
        allRoles.add(Rights.ROLE_INWARD_CREATE);
        allRoles.add(Rights.ROLE_INWARD_UPDATE);
        allRoles.add(Rights.ROLE_INWARD_DELETE);
        // e-TRF Management
        allRoles.add(Rights.ROLE_TRF_VIEW);
        allRoles.add(Rights.ROLE_TRF_CREATE);
        allRoles.add(Rights.ROLE_TRF_UPDATE);
        allRoles.add(Rights.ROLE_TRF_DELETE);
        allRoles.add(Rights.ROLE_TRF_ACCEPT);
        allRoles.add(Rights.ROLE_TRF_REJECT);
        allRoles.add(Rights.ROLE_TRF_ASSIGN_PARAMETERS);
        // Receipt Checklist
        allRoles.add(Rights.ROLE_RECEIPT_CHECKLIST_VIEW);
        allRoles.add(Rights.ROLE_RECEIPT_CHECKLIST_CREATE);
        allRoles.add(Rights.ROLE_RECEIPT_CHECKLIST_UPDATE);
        // Dashboard & Analytics
        allRoles.add(Rights.ROLE_DASHBOARD_VIEW);
        allRoles.add(Rights.ROLE_ANALYTICS_VIEW);
        // External Interface Hooks
        allRoles.add(Rights.ROLE_EXTERNAL_TOS_TRIGGER);
        allRoles.add(Rights.ROLE_EXTERNAL_TOS_UPDATE);
        // QC Inventory & Consumption Reconciliation
        allRoles.add(Rights.ROLE_INVENTORY_READ);
        allRoles.add(Rights.ROLE_INVENTORY_CREATE);
        allRoles.add(Rights.ROLE_INVENTORY_UPDATE);
        allRoles.add(Rights.ROLE_INVENTORY_DELETE);
        allRoles.add(Rights.ROLE_INVENTORY_ALLOCATE);
        allRoles.add(Rights.ROLE_INVENTORY_CONSUME);
        allRoles.add(Rights.ROLE_INVENTORY_VARIANCE);
        // Storage Allocation
        allRoles.add(Rights.ROLE_STORAGE_ASSIGN);
        allRoles.add(Rights.ROLE_STORAGE_VIEW);
        // Barcoding
        allRoles.add(Rights.ROLE_BARCODE_GENERATE);
        allRoles.add(Rights.ROLE_BARCODE_PRINT);
        // Test Allocation & Execution
        allRoles.add(Rights.ROLE_TEST_VIEW);
        allRoles.add(Rights.ROLE_TEST_ALLOCATE);
        allRoles.add(Rights.ROLE_TEST_EXECUTE);
        allRoles.add(Rights.ROLE_TEST_REASSIGN);
        allRoles.add(Rights.ROLE_TEST_HOLD);
        allRoles.add(Rights.ROLE_TEST_CANCEL);
        allRoles.add(Rights.ROLE_TEST_SUBMIT);
        allRoles.add(Rights.ROLE_TEST_APPROVE);
        // AR Lifecycle Management
        allRoles.add(Rights.ROLE_AR_CLOSE);
        allRoles.add(Rights.ROLE_AR_CANCEL);
        allRoles.add(Rights.ROLE_AR_STATUS_VIEW);
        // Review Workflow
        allRoles.add(Rights.ROLE_QC_HEAD_REVIEW);
        allRoles.add(Rights.ROLE_QA_HEAD_REVIEW);
        allRoles.add(Rights.ROLE_REVIEW_HISTORY_VIEW);
        // OOS Management
        allRoles.add(Rights.ROLE_OOS_RAISE);
        allRoles.add(Rights.ROLE_OOS_INVESTIGATE);
        allRoles.add(Rights.ROLE_OOS_RETEST);
        allRoles.add(Rights.ROLE_BATCH_REJECT);
        allRoles.add(Rights.ROLE_OOS_VIEW);
        // Trial Test & For Information
        allRoles.add(Rights.ROLE_TRIAL_TEST_CLOSE);
        allRoles.add(Rights.ROLE_AR_GENERATE_FRESH);
        // Subcontracting Module
        allRoles.add(Rights.ROLE_SUBCONTRACT_CREATE);
        allRoles.add(Rights.ROLE_SUBCONTRACT_VIEW);
        allRoles.add(Rights.ROLE_SUBCONTRACT_UPDATE);
        // Document Management
        allRoles.add(Rights.ROLE_DOCUMENT_UPLOAD);
        allRoles.add(Rights.ROLE_DOCUMENT_VIEW);

        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin")
            .authorities(allRoles.toArray(new String[0]))
            .build());
        return manager;
    }
}
