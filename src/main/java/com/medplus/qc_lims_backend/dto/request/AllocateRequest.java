package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for allocating sample quantity (SRS Module 5.2).
 * Split from combined allocation/consumption DTO per review MEDIUM correction.
 *
 * B-3 fix: allocatedToAnalystId removed — deferred to Module 6 (Test Allocation & Execution)
 * per SRS. The test_execution table in Module 6 will track analyst-level allocations.
 */
@Getter
@Setter
public class AllocateRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String remarks;
}
