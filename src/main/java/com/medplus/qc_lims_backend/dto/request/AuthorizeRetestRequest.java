package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for authorizing a retest (disposition update to Retest_Authorized).
 */
public record AuthorizeRetestRequest(
        String investigationSummary,
        String rootCause,
        String correctiveAction
) {
}
