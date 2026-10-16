package com.medplus.qc_lims_backend.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArStatus enum — lifecycle states for SampleInwardRegister.
 */
class ArStatusTest {

    @Test
    void dbValuesMatchEnum() {
        assertEquals("REGISTERED", ArStatus.REGISTERED.getDbValue());
        assertEquals("IN_PROGRESS", ArStatus.IN_PROGRESS.getDbValue());
        assertEquals("ALL_TESTS_COMPLETE", ArStatus.ALL_TESTS_COMPLETE.getDbValue());
        assertEquals("COMPLETED", ArStatus.COMPLETED.getDbValue());
        assertEquals("CANCELLED", ArStatus.CANCELLED.getDbValue());
    }

    @Test
    void fromDbValueResolvesCorrectly() {
        assertEquals(ArStatus.REGISTERED, ArStatus.fromDbValue("REGISTERED"));
        assertEquals(ArStatus.IN_PROGRESS, ArStatus.fromDbValue("IN_PROGRESS"));
        assertEquals(ArStatus.ALL_TESTS_COMPLETE, ArStatus.fromDbValue("ALL_TESTS_COMPLETE"));
        assertEquals(ArStatus.COMPLETED, ArStatus.fromDbValue("COMPLETED"));
        assertEquals(ArStatus.CANCELLED, ArStatus.fromDbValue("CANCELLED"));
    }

    @Test
    void fromDbValueReturnsNullForUnknown() {
        assertNull(ArStatus.fromDbValue("Unknown"));
        assertNull(ArStatus.fromDbValue(null));
    }

    @Test
    void activeStatuses() {
        assertTrue(ArStatus.REGISTERED.isActive());
        assertTrue(ArStatus.IN_PROGRESS.isActive());
        assertTrue(ArStatus.ALL_TESTS_COMPLETE.isActive());
    }

    @Test
    void terminalStatuses() {
        assertTrue(ArStatus.COMPLETED.isTerminal());
        assertTrue(ArStatus.CANCELLED.isTerminal());
        assertFalse(ArStatus.REGISTERED.isTerminal());
        assertFalse(ArStatus.IN_PROGRESS.isTerminal());
        assertFalse(ArStatus.ALL_TESTS_COMPLETE.isTerminal());
    }
}
