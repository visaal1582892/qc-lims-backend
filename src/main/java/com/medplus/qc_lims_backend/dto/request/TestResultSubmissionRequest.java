package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for submitting test results (SRS Module 6.2).
 *
 * Endpoint: POST /api/test-executions/{id}/submit-result
 */
@Getter
@Setter
public class TestResultSubmissionRequest {

    private BigDecimal resultNumericValue;

    private String resultTextValue;

    @NotNull(message = "Consumed units is required")
    @Min(value = 0, message = "Consumed units must be non-negative")
    private Integer consumedUnits;

    private String validityStatus;

    private String complianceFlag;
}
