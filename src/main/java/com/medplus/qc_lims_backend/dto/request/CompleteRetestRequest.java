package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for completing a retest with the revised result.
 */
public record CompleteRetestRequest(
        @NotBlank String revisedResult,
        String revisedResultUnit,
        String investigationSummary,
        String rootCause,
        String correctiveAction
) {
}
