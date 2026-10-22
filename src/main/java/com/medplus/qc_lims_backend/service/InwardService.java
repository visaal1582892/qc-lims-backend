package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.ArLifecycleRequest;
import com.medplus.qc_lims_backend.dto.request.CreateInwardRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateInwardRequest;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;

import java.util.List;

/**
 * Service layer interface for Inward Registration CRUD operations.
 */
public interface InwardService {

    List<InwardResponse> listAll();

    InwardResponse getById(Long id);

    InwardResponse getByArNumber(String arNumber);

    List<InwardResponse> listByProductId(Integer productId);

    List<InwardResponse> searchByBatchNo(String batchNo);

    List<InwardResponse> listByArStatus(String arStatus);

    InwardResponse create(CreateInwardRequest request, Long createdBy);

    InwardResponse update(Long id, UpdateInwardRequest request, Long updatedBy);

    void delete(Long id);

    /**
     * Close an AR — allowed only when all allocated tests are terminal.
     * Transition: ALL_TESTS_COMPLETE -> COMPLETED.
     */
    InwardResponse closeAr(Long id, ArLifecycleRequest request, Long qcHeadId);

    /**
     * Cancel an AR — allowed from REGISTERED or IN_PROGRESS.
     * Transition: REGISTERED/IN_PROGRESS -> CANCELLED.
     */
    InwardResponse cancelAr(Long id, ArLifecycleRequest request, Long qcHeadId);

    /**
     * Recalculate and persist the test completion rate for an AR.
     * Called by TestExecutionService after each test status change.
     */
    void updateTestCompletionAndStatus(String arNumber, long totalTests, long terminalTests);
}
