package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for status transitions (Hold, Cancel).
 *
 * Endpoints: POST /api/test-executions/{id}/hold, POST /api/test-executions/{id}/cancel
 */
@Getter
@Setter
public class TestStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String newStatus;

    private String reason;
}
