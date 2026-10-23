package com.medplus.qc_lims_backend.security;

/**
 * Canonical right authority strings — must match {@code rights.right_id} in the database.
 */
public final class Rights {

    private Rights() {}

    // Inward
    public static final String INWARD_CREATE        = "INWARD_CREATE";
    public static final String INWARD_READ          = "INWARD_READ";
    public static final String INWARD_UPDATE        = "INWARD_UPDATE";
    public static final String INWARD_DELETE        = "INWARD_DELETE";
    public static final String INWARD_APPROVE       = "INWARD_APPROVE";

    // Samples
    public static final String SAMPLE_VIEW          = "SAMPLE_VIEW";
    public static final String SAMPLE_UPDATE        = "SAMPLE_UPDATE";

    // Results
    public static final String RESULT_SUBMIT        = "RESULT_SUBMIT";
    public static final String RESULT_VIEW          = "RESULT_VIEW";
    public static final String RESULT_REVIEW        = "RESULT_REVIEW";

    // TRF
    public static final String TRF_GENERATE         = "TRF_GENERATE";
    public static final String TRF_APPROVE          = "TRF_APPROVE";
    public static final String TRF_VIEW             = "TRF_VIEW";

    // Certificates
    public static final String CERTIFICATE_GENERATE = "CERTIFICATE_GENERATE";
    public static final String CERTIFICATE_APPROVE  = "CERTIFICATE_APPROVE";

    // OOS
    public static final String OOS_INITIATE         = "OOS_INITIATE";
    public static final String OOS_INVESTIGATE      = "OOS_INVESTIGATE";
    public static final String OOS_REVIEW           = "OOS_REVIEW";
    public static final String OOS_APPROVE          = "OOS_APPROVE";

    // OOT
    public static final String OOT_INITIATE         = "OOT_INITIATE";
    public static final String OOT_INVESTIGATE      = "OOT_INVESTIGATE";
    public static final String OOT_REVIEW           = "OOT_REVIEW";
    public static final String OOT_APPROVE          = "OOT_APPROVE";

    // Dashboard / Analytics / Audit
    public static final String DASHBOARD_VIEW       = "DASHBOARD_VIEW";
    public static final String ANALYTICS_VIEW       = "ANALYTICS_VIEW";
    public static final String AUDIT_TRAIL_VIEW     = "AUDIT_TRAIL_VIEW";

    // User Management
    public static final String USER_CREATE          = "USER_CREATE";
    public static final String USER_READ            = "USER_READ";
    public static final String USER_UPDATE          = "USER_UPDATE";
    public static final String USER_DELETE          = "USER_DELETE";

    // Role Management
    public static final String ROLE_CREATE          = "ROLE_CREATE";
    public static final String ROLE_READ            = "ROLE_READ";
    public static final String ROLE_UPDATE          = "ROLE_UPDATE";
    public static final String ROLE_DELETE          = "ROLE_DELETE";

    // Master Data
    public static final String MASTER_DATA_READ     = "MASTER_DATA_READ";
    public static final String MASTER_DATA_CREATE   = "MASTER_DATA_CREATE";
    public static final String MASTER_DATA_UPDATE   = "MASTER_DATA_UPDATE";
    public static final String MASTER_DATA_DELETE   = "MASTER_DATA_DELETE";

    // Routing Config
    public static final String RACING_CONFIG_READ   = "RACING_CONFIG_READ";
    public static final String RACING_CONFIG_UPDATE = "RACING_CONFIG_UPDATE";

    // Personal
    public static final String CHANGE_OWN_PASSWORD  = "CHANGE_OWN_PASSWORD";
    public static final String VIEW_OWN_NOTIFICATIONS = "VIEW_OWN_NOTIFICATIONS";
}
