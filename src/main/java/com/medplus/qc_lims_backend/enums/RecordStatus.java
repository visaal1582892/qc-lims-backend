package com.medplus.qc_lims_backend.enums;

/**
 * Canonical record lifecycle status.
 * Used by BaseEntity and all domain entities that support soft-delete or archival.
 */
public enum RecordStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
