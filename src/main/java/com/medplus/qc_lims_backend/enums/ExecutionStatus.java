package com.medplus.qc_lims_backend.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Canonical execution status states for test execution lifecycle (SRS Module 6.2).
 *
 * State transition matrix:
 *   Allocated -> Testing, Hold, Cancelled_Not_Performed
 *   Testing   -> Hold, Under_Review, Cancelled_Not_Performed
 *   Hold      -> Testing, Cancelled_Not_Performed
 *   Cancelled_Not_Performed -> (terminal)
 *   Under_Review -> Released, Rework_Requested
 *   Rework_Requested -> Testing
 *   Released -> (terminal)
 *
 * String values match DB CHECK constraint in test_execution table.
 */
public enum ExecutionStatus {

    ALLOCATED("Allocated"),
    TESTING("Testing"),
    HOLD("Hold"),
    CANCELLED_NOT_PERFORMED("Cancelled_Not_Performed"),
    UNDER_REVIEW("Under_Review"),
    REWORK_REQUESTED("Rework_Requested"),
    RELEASED("Released");

    private final String dbValue;

    ExecutionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * Returns the database-stored string value for this status.
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Resolve enum from DB string value.
     * Returns null if no matching status found.
     */
    public static ExecutionStatus fromDbValue(String dbValue) {
        if (dbValue == null) return null;
        for (ExecutionStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Check if a transition from this status to the target status is allowed.
     */
    public boolean canTransitionTo(ExecutionStatus target) {
        return ALLOWED_TRANSITIONS.containsKey(this)
                && ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * Whether a reason text is required when transitioning to this status.
     */
    public boolean requiresReasonOnEntry() {
        return this == HOLD || this == CANCELLED_NOT_PERFORMED;
    }

    /**
     * Whether this is a terminal state (no outgoing transitions).
     * RELEASED can now transition to REWORK_REQUESTED (per H-02 correction),
     * so it is no longer terminal.
     */
    public boolean isTerminal() {
        return this == CANCELLED_NOT_PERFORMED;
    }

    /**
     * Allowed state transitions: from -> set of allowed targets.
     */
    private static final java.util.Map<ExecutionStatus, Set<ExecutionStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new java.util.EnumMap<>(ExecutionStatus.class);
        ALLOWED_TRANSITIONS.put(ALLOCATED, EnumSet.of(TESTING, HOLD, CANCELLED_NOT_PERFORMED));
        ALLOWED_TRANSITIONS.put(TESTING, EnumSet.of(HOLD, UNDER_REVIEW, CANCELLED_NOT_PERFORMED));
        ALLOWED_TRANSITIONS.put(HOLD, EnumSet.of(TESTING, CANCELLED_NOT_PERFORMED));
        ALLOWED_TRANSITIONS.put(UNDER_REVIEW, EnumSet.of(RELEASED, REWORK_REQUESTED));
        ALLOWED_TRANSITIONS.put(REWORK_REQUESTED, EnumSet.of(TESTING));
        // Terminal states have no outgoing transitions:
        ALLOWED_TRANSITIONS.put(CANCELLED_NOT_PERFORMED, EnumSet.noneOf(ExecutionStatus.class));
        ALLOWED_TRANSITIONS.put(RELEASED, EnumSet.of(REWORK_REQUESTED));
    }

    /**
     * Check if a DB-stored status string represents a terminal state
     * for AR lifecycle purposes. Both Released and Cancelled_Not_Performed
     * count as terminal for determining AR completion rate and status.
     * Note: This differs from {@link #isTerminal()} which tracks state machine
     * transitions — RELEASED can now go to REWORK_REQUESTED.
     */
    public static boolean isTerminalState(String status) {
        return RELEASED.getDbValue().equals(status)
                || CANCELLED_NOT_PERFORMED.getDbValue().equals(status);
    }

    /**
     * Check if a DB-stored status string is non-terminal (test still active).
     */
    public static boolean isNonTerminalState(String status) {
        return !isTerminalState(status);
    }
}
