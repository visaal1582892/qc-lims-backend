package com.medplus.qc_lims_backend.enums;

/**
 * AR (Analysis Request) lifecycle status for SampleInwardRegister.
 *
 * Progression:
 *   REGISTERED          — sample registered, no tests allocated yet
 *   IN_PROGRESS         — at least one test allocated but not all terminal
 *   ALL_TESTS_COMPLETE  — all allocated tests reached a terminal state
 *   COMPLETED           — AR closed / archived by QC Head
 *   CANCELLED           — AR cancelled before meaningful testing
 *
 * Transitions are managed by {@code SampleInwardService}.
 */
public enum ArStatus {

    REGISTERED("REGISTERED"),
    IN_PROGRESS("IN_PROGRESS"),
    ALL_TESTS_COMPLETE("ALL_TESTS_COMPLETE"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String dbValue;

    ArStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /**
     * Resolve enum from DB string value.
     */
    public static ArStatus fromDbValue(String dbValue) {
        if (dbValue == null) return null;
        for (ArStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Active statuses that should appear in normal workflow queries.
     */
    public boolean isActive() {
        return this == REGISTERED || this == IN_PROGRESS || this == ALL_TESTS_COMPLETE;
    }

    /**
     * Terminal statuses that close the AR lifecycle.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
