package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.ReceiptChecklistRequest;
import com.medplus.qc_lims_backend.dto.response.ReceiptChecklistResponse;

/**
 * Service layer interface for physical verification receipt checklists (SRS 3.1 D).
 *
 * Enforces one checklist per TRF (V-20).
 */
public interface ReceiptChecklistService {

    /**
     * Create a new receipt checklist for a TRF.
     * Enforces V-20: exactly one checklist per TRF.
     */
    ReceiptChecklistResponse create(ReceiptChecklistRequest request, Long createdBy);

    /**
     * Update an existing receipt checklist.
     */
    ReceiptChecklistResponse update(Long checklistId, ReceiptChecklistRequest request, Long updatedBy);

    /**
     * Get the receipt checklist by TRF ID.
     */
    ReceiptChecklistResponse getByTrfId(Integer trfId);

    /**
     * Resolve current user ID from Spring Security context.
     */
    Long resolveUserId();
}
