package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting a batch after investigation.
 */
public record RejectBatchRequest(
        String investigationSummary,
        String rootCause,
        String correctiveAction
) {
}
