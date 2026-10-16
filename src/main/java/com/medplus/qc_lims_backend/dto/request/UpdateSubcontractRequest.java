package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating subcontract remarks/status (SRS Module 10).
 */
public record UpdateSubcontractRequest(
        /** Remarks to add to the subcontract entry */
        String remarks,

        /** New status (DISPATCHED, IN_PROGRESS, COMPLETED, CANCELLED) */
        String status
) {}
