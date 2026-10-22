package com.medplus.qc_lims_backend.service;

/**
 * Service layer interface for atomic AR number generation.
 *
 * Uses INSERT ... ON DUPLICATE KEY UPDATE SET last_sequence = last_sequence + 1
 * followed by SELECT — a single atomic statement with no race condition (RC-4).
 *
 * Calendar year stored as 2-digit string ('26') consistent with V1 seed.
 */
public interface ArSequenceGenerator {

    /**
     * Generate a unique AR number (e.g. MHSL260001).
     */
    String generateArNumber();

    /**
     * Generate an OOS incident number (e.g. OOS-260001).
     */
    String generateOosNumber();
}
