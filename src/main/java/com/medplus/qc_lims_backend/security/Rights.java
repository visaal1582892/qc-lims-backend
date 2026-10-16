package com.medplus.qc_lims_backend.security;

/**
 * Centralized rights constants per functional rights binding architecture.
 *
 * All rights use the ROLE_ prefix for Spring Security compatibility.
 * No inline string literals for rights — use constants from this class.
 *
 * Rights are added here as features are implemented in subsequent phases.
 * Each new right must be tracked in docs/rights_tracking.md.
 */
public final class Rights {

    private Rights() {
        // Prevent instantiation — utility class with static constants only
    }

    // Rights will be added by feature phase (e.g., ROLE_INWARD_VIEW, ROLE_TEST_EXECUTE)

    // Test Parameter Master (Phase 1) — SRS Module 1
    public static final String ROLE_TEST_PARAMETER_READ   = "ROLE_TEST_PARAMETER_READ";
    public static final String ROLE_TEST_PARAMETER_CREATE = "ROLE_TEST_PARAMETER_CREATE";
    public static final String ROLE_TEST_PARAMETER_UPDATE = "ROLE_TEST_PARAMETER_UPDATE";
    public static final String ROLE_TEST_PARAMETER_DELETE = "ROLE_TEST_PARAMETER_DELETE";
}
