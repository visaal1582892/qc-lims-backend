package com.medplus.qc_lims_backend.enums;

/**
 * Canonical record lifecycle status.
 * Used by BaseEntity and all domain entities that support soft-delete or archival.
 */
public enum RecordStatus {
    ACTIVE,
    INACTIVE,
    DELETED;

    /**
     * Return the database string for this status.
     */
    public String getDbValue() {
        return this.name();
    }

    /**
     * Find enum by database value (case-insensitive).
     * Defaults to ACTIVE for null/unknown.
     */
    public static RecordStatus fromDbValue(String value) {
        if (value == null) return ACTIVE;
        for (RecordStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) return status;
        }
        return ACTIVE;
    }
}
