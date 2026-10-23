package com.medplus.qc_lims_backend.security;

/**
 * Canonical right authority strings — must match {@code rights.right_id} in the database.
 * All rights MUST begin with the {@code ROLE_} prefix (goal.md convention).
 * Rights-based access control: backend controllers use {@code @PreAuthorize("hasAuthority('ROLE_...')")}
 * and frontend checks {@code user.rights} array from login response.
 */
public final class Rights {

    private Rights() {}

    /** Full system access — grants all rights. Mapped to ADMIN role. */
    public static final String ROLE_SYSTEM_ADMIN   = "ROLE_SYSTEM_ADMIN";

    // --- Personal ---
    public static final String ROLE_CHANGE_OWN_PASSWORD = "ROLE_CHANGE_OWN_PASSWORD";
    public static final String ROLE_VIEW_OWN_NOTIFICATIONS = "ROLE_VIEW_OWN_NOTIFICATIONS";

    // --- User Management ---
    public static final String ROLE_USER_CREATE  = "ROLE_USER_CREATE";
    public static final String ROLE_USER_READ    = "ROLE_USER_READ";
    public static final String ROLE_USER_UPDATE  = "ROLE_USER_UPDATE";
    public static final String ROLE_USER_DELETE  = "ROLE_USER_DELETE";

    // --- Role Management ---
    public static final String ROLE_ROLE_CREATE  = "ROLE_ROLE_CREATE";
    public static final String ROLE_ROLE_READ    = "ROLE_ROLE_READ";
    public static final String ROLE_ROLE_UPDATE  = "ROLE_ROLE_UPDATE";

    // --- Master Data ---
    public static final String ROLE_MASTER_DATA_READ   = "ROLE_MASTER_DATA_READ";
    public static final String ROLE_MASTER_DATA_CREATE = "ROLE_MASTER_DATA_CREATE";
    public static final String ROLE_MASTER_DATA_UPDATE = "ROLE_MASTER_DATA_UPDATE";
    public static final String ROLE_MASTER_DATA_DELETE = "ROLE_MASTER_DATA_DELETE";

    // --- Metric Unit Master ---
    public static final String ROLE_METRIC_UNIT_READ   = "ROLE_METRIC_UNIT_READ";
    public static final String ROLE_METRIC_UNIT_UPLOAD = "ROLE_METRIC_UNIT_UPLOAD";

    // --- Test Parameter Master ---
    public static final String ROLE_TEST_PARAMETER_READ   = "ROLE_TEST_PARAMETER_READ";
    public static final String ROLE_TEST_PARAMETER_UPLOAD = "ROLE_TEST_PARAMETER_UPLOAD";

    // --- Analyst Qualification Master ---
    public static final String ROLE_QUALIFICATION_READ   = "ROLE_QUALIFICATION_READ";
    public static final String ROLE_QUALIFICATION_UPLOAD = "ROLE_QUALIFICATION_UPLOAD";

    // --- QC Inventory Configuration ---
    public static final String ROLE_INVENTORY_CONFIG_READ   = "ROLE_INVENTORY_CONFIG_READ";
    public static final String ROLE_INVENTORY_CONFIG_UPLOAD = "ROLE_INVENTORY_CONFIG_UPLOAD";
}
