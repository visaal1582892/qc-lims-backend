package com.medplus.qc_lims_backend.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-15: ExecutionStatus canTransitionTo() — full matrix.
 * Verifies the state machine transition rules match SRS Module 6.2.
 */
class ExecutionStatusTest {

    // --- Terminal states ---

    @Test
    void cancelledNotPerformedIsTerminal() {
        assertTrue(ExecutionStatus.CANCELLED_NOT_PERFORMED.isTerminal());
        // No outgoing transitions
        for (ExecutionStatus target : ExecutionStatus.values()) {
            assertFalse(ExecutionStatus.CANCELLED_NOT_PERFORMED.canTransitionTo(target),
                    "Cancelled_Not_Performed should not transition to " + target);
        }
    }

    @Test
    void releasedCanGoToReworkRequested() {
        // H-02 correction: RELEASED -> REWORK_REQUESTED is now allowed
        assertTrue(ExecutionStatus.RELEASED.canTransitionTo(ExecutionStatus.REWORK_REQUESTED));
    }

    @Test
    void releasedCannotGoToTesting() {
        assertFalse(ExecutionStatus.RELEASED.canTransitionTo(ExecutionStatus.TESTING));
    }

    // --- Allocated transitions ---

    @Test
    void allocatedCanGoToTesting() {
        assertTrue(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.TESTING));
    }

    @Test
    void allocatedCanGoToHold() {
        assertTrue(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.HOLD));
    }

    @Test
    void allocatedCanGoToCancelledNotPerformed() {
        assertTrue(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.CANCELLED_NOT_PERFORMED));
    }

    @Test
    void allocatedCannotGoToUnderReview() {
        assertFalse(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.UNDER_REVIEW));
    }

    @Test
    void allocatedCannotGoToReleased() {
        assertFalse(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.RELEASED));
    }

    @Test
    void allocatedCannotGoToReworkRequested() {
        assertFalse(ExecutionStatus.ALLOCATED.canTransitionTo(ExecutionStatus.REWORK_REQUESTED));
    }

    // --- Testing transitions ---

    @Test
    void testingCanGoToHold() {
        assertTrue(ExecutionStatus.TESTING.canTransitionTo(ExecutionStatus.HOLD));
    }

    @Test
    void testingCanGoToUnderReview() {
        assertTrue(ExecutionStatus.TESTING.canTransitionTo(ExecutionStatus.UNDER_REVIEW));
    }

    @Test
    void testingCanGoToCancelledNotPerformed() {
        assertTrue(ExecutionStatus.TESTING.canTransitionTo(ExecutionStatus.CANCELLED_NOT_PERFORMED));
    }

    @Test
    void testingCannotGoToReleased() {
        assertFalse(ExecutionStatus.TESTING.canTransitionTo(ExecutionStatus.RELEASED));
    }

    // --- Hold transitions ---

    @Test
    void holdCanGoToTesting() {
        assertTrue(ExecutionStatus.HOLD.canTransitionTo(ExecutionStatus.TESTING));
    }

    @Test
    void holdCanGoToCancelledNotPerformed() {
        assertTrue(ExecutionStatus.HOLD.canTransitionTo(ExecutionStatus.CANCELLED_NOT_PERFORMED));
    }

    @Test
    void holdCannotGoToUnderReview() {
        assertFalse(ExecutionStatus.HOLD.canTransitionTo(ExecutionStatus.UNDER_REVIEW));
    }

    // --- Under_Review transitions ---

    @Test
    void underReviewCanGoToReleased() {
        assertTrue(ExecutionStatus.UNDER_REVIEW.canTransitionTo(ExecutionStatus.RELEASED));
    }

    @Test
    void underReviewCanGoToReworkRequested() {
        assertTrue(ExecutionStatus.UNDER_REVIEW.canTransitionTo(ExecutionStatus.REWORK_REQUESTED));
    }

    @Test
    void underReviewCannotGoToTesting() {
        assertFalse(ExecutionStatus.UNDER_REVIEW.canTransitionTo(ExecutionStatus.TESTING));
    }

    // --- Rework_Requested transitions ---

    @Test
    void reworkRequestedCanGoToTesting() {
        assertTrue(ExecutionStatus.REWORK_REQUESTED.canTransitionTo(ExecutionStatus.TESTING));
    }

    @Test
    void reworkRequestedCannotGoToReleased() {
        assertFalse(ExecutionStatus.REWORK_REQUESTED.canTransitionTo(ExecutionStatus.RELEASED));
    }

    @Test
    void reworkRequestedCannotGoToHold() {
        assertFalse(ExecutionStatus.REWORK_REQUESTED.canTransitionTo(ExecutionStatus.HOLD));
    }

    // --- DB value mapping ---

    @Test
    void dbValuesMatchSrsSpec() {
        assertEquals("Allocated", ExecutionStatus.ALLOCATED.getDbValue());
        assertEquals("Testing", ExecutionStatus.TESTING.getDbValue());
        assertEquals("Hold", ExecutionStatus.HOLD.getDbValue());
        assertEquals("Cancelled_Not_Performed", ExecutionStatus.CANCELLED_NOT_PERFORMED.getDbValue());
        assertEquals("Under_Review", ExecutionStatus.UNDER_REVIEW.getDbValue());
        assertEquals("Rework_Requested", ExecutionStatus.REWORK_REQUESTED.getDbValue());
        assertEquals("Released", ExecutionStatus.RELEASED.getDbValue());
    }

    @Test
    void fromDbValueResolvesCorrectly() {
        assertEquals(ExecutionStatus.ALLOCATED, ExecutionStatus.fromDbValue("Allocated"));
        assertEquals(ExecutionStatus.TESTING, ExecutionStatus.fromDbValue("Testing"));
        assertEquals(ExecutionStatus.HOLD, ExecutionStatus.fromDbValue("Hold"));
        assertEquals(ExecutionStatus.CANCELLED_NOT_PERFORMED, ExecutionStatus.fromDbValue("Cancelled_Not_Performed"));
        assertEquals(ExecutionStatus.UNDER_REVIEW, ExecutionStatus.fromDbValue("Under_Review"));
        assertEquals(ExecutionStatus.REWORK_REQUESTED, ExecutionStatus.fromDbValue("Rework_Requested"));
        assertEquals(ExecutionStatus.RELEASED, ExecutionStatus.fromDbValue("Released"));
    }

    @Test
    void fromDbValueReturnsNullForUnknown() {
        assertNull(ExecutionStatus.fromDbValue("Unknown"));
        assertNull(ExecutionStatus.fromDbValue(null));
    }

    // --- isTerminal checks ---

    @Test
    void cancelledIsTerminal() {
        assertTrue(ExecutionStatus.CANCELLED_NOT_PERFORMED.isTerminal());
    }

    @Test
    void releasedIsNotTerminalAnymore() {
        // H-02: RELEASED can now transition to REWORK_REQUESTED
        assertFalse(ExecutionStatus.RELEASED.isTerminal());
    }

    // --- requiresReasonOnEntry ---

    @Test
    void holdAndCancelRequireReason() {
        assertTrue(ExecutionStatus.HOLD.requiresReasonOnEntry());
        assertTrue(ExecutionStatus.CANCELLED_NOT_PERFORMED.requiresReasonOnEntry());
    }

    @Test
    void otherStatusesDoNotRequireReason() {
        assertFalse(ExecutionStatus.ALLOCATED.requiresReasonOnEntry());
        assertFalse(ExecutionStatus.TESTING.requiresReasonOnEntry());
        assertFalse(ExecutionStatus.UNDER_REVIEW.requiresReasonOnEntry());
        assertFalse(ExecutionStatus.RELEASED.requiresReasonOnEntry());
        assertFalse(ExecutionStatus.REWORK_REQUESTED.requiresReasonOnEntry());
    }
}
