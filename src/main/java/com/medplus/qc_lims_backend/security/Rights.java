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

    // Sample Inward Register (Phase 2) — SRS Module 2
    public static final String ROLE_INWARD_VIEW   = "ROLE_INWARD_VIEW";
    public static final String ROLE_INWARD_CREATE = "ROLE_INWARD_CREATE";
    public static final String ROLE_INWARD_UPDATE = "ROLE_INWARD_UPDATE";
    public static final String ROLE_INWARD_DELETE = "ROLE_INWARD_DELETE";

    // e-TRF Management (Phase 2) — SRS Module 3
    public static final String ROLE_TRF_VIEW             = "ROLE_TRF_VIEW";
    public static final String ROLE_TRF_CREATE           = "ROLE_TRF_CREATE";
    public static final String ROLE_TRF_UPDATE           = "ROLE_TRF_UPDATE";
    public static final String ROLE_TRF_DELETE           = "ROLE_TRF_DELETE";
    public static final String ROLE_TRF_ACCEPT           = "ROLE_TRF_ACCEPT";
    public static final String ROLE_TRF_REJECT           = "ROLE_TRF_REJECT";
    public static final String ROLE_TRF_ASSIGN_PARAMETERS = "ROLE_TRF_ASSIGN_PARAMETERS";

    // Receipt Checklist (Phase 2) — SRS Module 3 (physical verification)
    public static final String ROLE_RECEIPT_CHECKLIST_VIEW   = "ROLE_RECEIPT_CHECKLIST_VIEW";
    public static final String ROLE_RECEIPT_CHECKLIST_CREATE = "ROLE_RECEIPT_CHECKLIST_CREATE";
    public static final String ROLE_RECEIPT_CHECKLIST_UPDATE = "ROLE_RECEIPT_CHECKLIST_UPDATE";

    // QC Dashboard (Phase 8) — SRS Module 12.1
    public static final String ROLE_DASHBOARD_VIEW  = "ROLE_DASHBOARD_VIEW";

    // Multi-Dimensional Analytics (Phase 8) — SRS Module 12.2
    public static final String ROLE_ANALYTICS_VIEW  = "ROLE_ANALYTICS_VIEW";

    // External Interface Hooks (Phase 9) — SRS Module 14
    public static final String ROLE_EXTERNAL_TOS_TRIGGER = "ROLE_EXTERNAL_TOS_TRIGGER";
    public static final String ROLE_EXTERNAL_TOS_UPDATE  = "ROLE_EXTERNAL_TOS_UPDATE";

    // QC Inventory & Consumption Reconciliation (Phase 3) — SRS Module 5
    public static final String ROLE_INVENTORY_READ     = "ROLE_INVENTORY_READ";
    public static final String ROLE_INVENTORY_CREATE   = "ROLE_INVENTORY_CREATE";
    public static final String ROLE_INVENTORY_UPDATE   = "ROLE_INVENTORY_UPDATE";
    public static final String ROLE_INVENTORY_DELETE   = "ROLE_INVENTORY_DELETE";
    public static final String ROLE_INVENTORY_ALLOCATE = "ROLE_INVENTORY_ALLOCATE";
    public static final String ROLE_INVENTORY_CONSUME  = "ROLE_INVENTORY_CONSUME";
    public static final String ROLE_INVENTORY_VARIANCE = "ROLE_INVENTORY_VARIANCE";

    // Storage Allocation (Phase 3) — SRS Module 4
    public static final String ROLE_STORAGE_ASSIGN = "ROLE_STORAGE_ASSIGN";
    public static final String ROLE_STORAGE_VIEW   = "ROLE_STORAGE_VIEW";

    // Barcoding (Phase 3) — SRS Module 4
    public static final String ROLE_BARCODE_GENERATE = "ROLE_BARCODE_GENERATE";
    public static final String ROLE_BARCODE_PRINT    = "ROLE_BARCODE_PRINT";

    // Test Allocation & Execution State Machine (Phase 4) — SRS Module 6
    public static final String ROLE_TEST_VIEW     = "ROLE_TEST_VIEW";
    public static final String ROLE_TEST_ALLOCATE = "ROLE_TEST_ALLOCATE";
    public static final String ROLE_TEST_EXECUTE  = "ROLE_TEST_EXECUTE";
    public static final String ROLE_TEST_REASSIGN = "ROLE_TEST_REASSIGN";
    public static final String ROLE_TEST_HOLD     = "ROLE_TEST_HOLD";
    public static final String ROLE_TEST_CANCEL   = "ROLE_TEST_CANCEL";
    public static final String ROLE_TEST_SUBMIT   = "ROLE_TEST_SUBMIT";
    public static final String ROLE_TEST_APPROVE  = "ROLE_TEST_APPROVE";

    // AR Lifecycle Management (Phase 5) — SRS Module 7
    public static final String ROLE_AR_CLOSE      = "ROLE_AR_CLOSE";
    public static final String ROLE_AR_CANCEL     = "ROLE_AR_CANCEL";
    public static final String ROLE_AR_STATUS_VIEW = "ROLE_AR_STATUS_VIEW";
}
