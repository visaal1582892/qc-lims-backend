package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.CreateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.response.SubcontractResponse;

import java.util.List;

/**
 * Service layer interface for Subcontracting (SRS Module 10).
 *
 * Responsibilities:
 * - Create subcontract entries for external lab dispatch
 * - List subcontract register entries
 * - Update subcontract remarks and status
 * - NO AR Number generated (ar_number remains NULL)
 */
public interface SubcontractingService {

    /**
     * Create a new subcontract entry for external lab dispatch.
     */
    SubcontractResponse createSubcontract(CreateSubcontractRequest request);

    /**
     * List all active subcontract entries.
     */
    List<SubcontractResponse> listSubcontracts();

    /**
     * Get a single subcontract by ID.
     */
    SubcontractResponse getSubcontract(Long id);

    /**
     * Update subcontract remarks and status.
     */
    SubcontractResponse updateSubcontract(Long id, UpdateSubcontractRequest request);

    /**
     * Mark ROA as received for a subcontract.
     */
    SubcontractResponse markRoaReceived(Long id);

    /**
     * Resolve the current user ID from SecurityContextHolder.
     */
    Long resolveUserId();
}
