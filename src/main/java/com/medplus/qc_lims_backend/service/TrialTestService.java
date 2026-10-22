package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;

import java.util.List;

/**
 * Service layer interface for Trial Test and For Information (SRS Module 9).
 *
 * Responsibilities:
 * - Mark an AR as FOR_INFORMATION (trial test)
 * - Generate a fresh AR from a For_Information AR
 * - Link the fresh AR back to the For_Information AR via linkedForInfoArNumber
 * - Close the For_Information AR after fresh AR is generated
 */
public interface TrialTestService {

    /**
     * Mark an AR as FOR_INFORMATION (trial test).
     * Only allowed on ARs that are in COMPLETED or ALL_TESTS_COMPLETE state.
     */
    SampleInwardRegister markForInformation(String arNumber, String reason);

    /**
     * Generate a fresh AR from a For_Information AR.
     * Creates a new AR with a new AR number, linked back to the source.
     */
    SampleInwardRegister generateFreshAr(String forInfoArNumber);

    /**
     * Get a specific For_Information AR by number.
     */
    SampleInwardRegister getForInfoAr(String arNumber);

    /**
     * List all ARs currently in FOR_INFORMATION status.
     */
    List<SampleInwardRegister> listForInfoArs();

    /**
     * Resolve current user ID from SecurityContextHolder.
     */
    Long resolveUserId();
}
