package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for creating a subcontract entry (SRS Module 10).
 * NO AR Number generated — ar_number remains NULL.
 */
public record CreateSubcontractRequest(
        /** TRF ID reference (optional) */
        Integer trfId,

        /** External lab name (required) */
        @NotBlank(message = "External lab name is required")
        String externalLabName,

        /** Reason for subcontracting (required) */
        @NotBlank(message = "Reason is required")
        String reason,

        /** QC Head user ID who authorizes the dispatch */
        @NotNull(message = "QC Head ID is required")
        Long qcHeadId,

        /** Expected return date from external lab (optional) */
        LocalDate expectedReturnDate
) {}
