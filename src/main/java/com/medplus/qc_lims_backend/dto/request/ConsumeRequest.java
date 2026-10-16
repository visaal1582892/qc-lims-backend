package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for recording consumed sample quantity (SRS Module 5.2).
 * Split from combined allocation/consumption DTO per review MEDIUM correction.
 */
@Getter
@Setter
public class ConsumeRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String remarks;
}
