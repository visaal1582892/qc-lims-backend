package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for allocating a test to an analyst (SRS Module 6.1).
 *
 * Endpoint: POST /api/test-executions/allocate
 */
@Getter
@Setter
public class TestAllocationRequest {

    @NotBlank(message = "AR number is required")
    private String arNumber;

    @NotNull(message = "Parameter ID is required")
    private Integer parameterId;

    @NotNull(message = "Analyst ID is required")
    private Integer allocatedToAnalystId;

    @NotNull(message = "QC Head ID is required")
    private Integer assignedByQcHeadId;

    @NotNull(message = "Allocated units is required")
    @Min(value = 1, message = "Allocated units must be at least 1")
    private Integer allocatedUnits;
}
