package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for allocating sample quantity to an analyst (SRS Module 5.2).
 * Split from combined allocation/consumption DTO per review MEDIUM correction.
 */
@Getter
@Setter
public class AllocateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Analyst ID is required for allocation")
    private Integer allocatedToAnalystId;

    private String remarks;
}
