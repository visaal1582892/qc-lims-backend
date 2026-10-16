package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for reassigning a test to a different analyst (SRS Module 6.1).
 *
 * Endpoint: POST /api/test-executions/{id}/reassign
 */
@Getter
@Setter
public class TestReassignmentRequest {

    @NotNull(message = "New analyst ID is required")
    private Integer newAnalystId;

    @NotBlank(message = "Reassignment reason is required")
    private String reason;
}
