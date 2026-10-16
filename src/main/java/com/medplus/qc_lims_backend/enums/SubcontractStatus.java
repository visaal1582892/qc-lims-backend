package com.medplus.qc_lims_backend.enums;

/**
 * Subcontract workflow status values (SRS Module 10).
 *
 * Lifecycle: DISPATCHED → IN_PROGRESS → COMPLETED / CANCELLED.
 */
public enum SubcontractStatus {

    DISPATCHED("DISPATCHED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String dbValue;

    SubcontractStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static SubcontractStatus fromDbValue(String dbValue) {
        for (SubcontractStatus s : values()) {
            if (s.dbValue.equals(dbValue)) return s;
        }
        throw new IllegalArgumentException("Unknown SubcontractStatus: " + dbValue);
    }
}
