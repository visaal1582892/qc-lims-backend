package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.TrfCreateRequest;
import com.medplus.qc_lims_backend.dto.request.TrfUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TrfResponse;

import java.util.List;

/**
 * Service layer interface for e-TRF Management CRUD and lifecycle operations.
 *
 * Handles lifecycle: Pending_Feasibility -> Accepted/Rejected (SRS 3.2).
 */
public interface TrfService {

    // --- READ operations ---

    List<TrfResponse> listAll();

    TrfResponse getById(Long id);

    List<TrfResponse> listByStatus(String trfStatus);

    List<TrfResponse> listByProductId(Integer productId);

    List<TrfResponse> searchByGrnNumber(String grnNumber);

    // --- CREATE (mutating) ---

    TrfResponse create(TrfCreateRequest request, Long createdBy);

    // --- UPDATE (mutating) ---

    TrfResponse update(Long id, TrfUpdateRequest request, Long updatedBy);

    // --- LIFECYCLE: Feasibility decision ---

    TrfResponse accept(Long id, Long updatedBy);

    TrfResponse reject(Long id, Long updatedBy);

    // --- SOFT DELETE ---

    void delete(Long id);

    // --- ASSIGN TEST PARAMETERS (QC Head customization) ---

    void assignTestParameters(Long trfId, List<Integer> parameterIds, Long updatedBy);
}
