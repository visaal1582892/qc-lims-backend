package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating an inward registration record.
 * Includes trf_id (nullable) per RC-6.
 */
@Getter
@Setter
public class CreateInwardRequest {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotBlank(message = "Batch number is required")
    @Size(max = 100, message = "Batch number must be at most 100 characters")
    private String batchNo;

    @NotNull(message = "Received quantity (packs) is required")
    @Min(value = 1, message = "Received quantity must be at least 1")
    private Integer receivedQuantityPacks;

    @NotNull(message = "Total received units is required")
    @Min(value = 1, message = "Total received units must be at least 1")
    private Integer totalReceivedUnits;

    private Integer trfId;

    @Size(max = 100, message = "Storage location must be at most 100 characters")
    @Pattern(regexp = "^(Ambient|Freezer|Refrigerator|Others)$",
             message = "Storage location must be one of: Ambient, Freezer, Refrigerator, Others")
    private String storageLocation;

    private Boolean isSubcontracted;

    private String subcontractRemarks;
}
