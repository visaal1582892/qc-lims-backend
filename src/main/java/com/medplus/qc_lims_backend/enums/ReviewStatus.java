package com.medplus.qc_lims_backend.enums;

/**
 * Review status values stored on test_execution.review_status.
 *
 * Tracks the multi-tier review pipeline position:
 *   PENDING_QC — submitted, awaiting QC Head review
 *   PENDING_QA — QC approved, awaiting QA Head review
 *   QC_CLEARED — QA approved, COA locked (final)
 *   REWORK_REQUESTED — rework sent back by reviewer
 */
public enum ReviewStatus {

    PENDING_QC("PENDING_QC"),
    PENDING_QA("PENDING_QA"),
    QC_CLEARED("QC_CLEARED"),
    REWORK_REQUESTED("REWORK_REQUESTED");

    private final String dbValue;

    ReviewStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /**
     * Resolve enum from DB string value.
     * Returns null if no matching status found.
     */
    public static ReviewStatus fromDbValue(String dbValue) {
        if (dbValue == null) return null;
        for (ReviewStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        return null;
    }
}
