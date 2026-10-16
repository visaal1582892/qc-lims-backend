package com.medplus.qc_lims_backend.config;

import com.medplus.qc_lims_backend.security.Rights;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            .httpBasic(Customizer.withDefaults())
            .anonymous(anonymous -> anonymous
                .authorities(allAuthorities().toArray(new String[0])));
        return http.build();
    }

    private List<String> allAuthorities() {
        List<String> list = new ArrayList<>();
        list.add(Rights.ROLE_TEST_PARAMETER_READ);
        list.add(Rights.ROLE_TEST_PARAMETER_CREATE);
        list.add(Rights.ROLE_TEST_PARAMETER_UPDATE);
        list.add(Rights.ROLE_TEST_PARAMETER_DELETE);
        list.add(Rights.ROLE_INWARD_VIEW);
        list.add(Rights.ROLE_INWARD_CREATE);
        list.add(Rights.ROLE_INWARD_UPDATE);
        list.add(Rights.ROLE_INWARD_DELETE);
        list.add(Rights.ROLE_TRF_VIEW);
        list.add(Rights.ROLE_TRF_CREATE);
        list.add(Rights.ROLE_TRF_UPDATE);
        list.add(Rights.ROLE_TRF_DELETE);
        list.add(Rights.ROLE_TRF_ACCEPT);
        list.add(Rights.ROLE_TRF_REJECT);
        list.add(Rights.ROLE_TRF_ASSIGN_PARAMETERS);
        list.add(Rights.ROLE_RECEIPT_CHECKLIST_VIEW);
        list.add(Rights.ROLE_RECEIPT_CHECKLIST_CREATE);
        list.add(Rights.ROLE_RECEIPT_CHECKLIST_UPDATE);
        list.add(Rights.ROLE_DASHBOARD_VIEW);
        list.add(Rights.ROLE_ANALYTICS_VIEW);
        list.add(Rights.ROLE_EXTERNAL_TOS_TRIGGER);
        list.add(Rights.ROLE_EXTERNAL_TOS_UPDATE);
        list.add(Rights.ROLE_INVENTORY_READ);
        list.add(Rights.ROLE_INVENTORY_CREATE);
        list.add(Rights.ROLE_INVENTORY_UPDATE);
        list.add(Rights.ROLE_INVENTORY_DELETE);
        list.add(Rights.ROLE_INVENTORY_ALLOCATE);
        list.add(Rights.ROLE_INVENTORY_CONSUME);
        list.add(Rights.ROLE_INVENTORY_VARIANCE);
        list.add(Rights.ROLE_STORAGE_ASSIGN);
        list.add(Rights.ROLE_STORAGE_VIEW);
        list.add(Rights.ROLE_BARCODE_GENERATE);
        list.add(Rights.ROLE_BARCODE_PRINT);
        list.add(Rights.ROLE_TEST_VIEW);
        list.add(Rights.ROLE_TEST_ALLOCATE);
        list.add(Rights.ROLE_TEST_EXECUTE);
        list.add(Rights.ROLE_TEST_REASSIGN);
        list.add(Rights.ROLE_TEST_HOLD);
        list.add(Rights.ROLE_TEST_CANCEL);
        list.add(Rights.ROLE_TEST_SUBMIT);
        list.add(Rights.ROLE_TEST_APPROVE);
        list.add(Rights.ROLE_AR_CLOSE);
        list.add(Rights.ROLE_AR_CANCEL);
        list.add(Rights.ROLE_AR_STATUS_VIEW);
        list.add(Rights.ROLE_QC_HEAD_REVIEW);
        list.add(Rights.ROLE_QA_HEAD_REVIEW);
        list.add(Rights.ROLE_REVIEW_HISTORY_VIEW);
        list.add(Rights.ROLE_OOS_RAISE);
        list.add(Rights.ROLE_OOS_INVESTIGATE);
        list.add(Rights.ROLE_OOS_RETEST);
        list.add(Rights.ROLE_BATCH_REJECT);
        list.add(Rights.ROLE_OOS_VIEW);
        list.add(Rights.ROLE_TRIAL_TEST_CLOSE);
        list.add(Rights.ROLE_AR_GENERATE_FRESH);
        list.add(Rights.ROLE_SUBCONTRACT_CREATE);
        list.add(Rights.ROLE_SUBCONTRACT_VIEW);
        list.add(Rights.ROLE_SUBCONTRACT_UPDATE);
        list.add(Rights.ROLE_DOCUMENT_UPLOAD);
        list.add(Rights.ROLE_DOCUMENT_VIEW);
        return list;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        List<String> allRoles = allAuthorities();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin")
            .authorities(allRoles.toArray(new String[0]))
            .build());
        return manager;
    }
}
