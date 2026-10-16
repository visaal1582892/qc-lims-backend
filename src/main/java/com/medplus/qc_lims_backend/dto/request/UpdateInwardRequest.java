package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating an inward registration record.
 * All fields optional for partial update, but productId and batchNo remain mandatory.
 */
@Getter
@Setter
public class UpdateInwardRequest {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @Size(max = 100, message = "Batch number must be at most 100 characters")
    private String batchNo;

    @Min(value = 1, message = "Received quantity must be at least 1")
    private Integer receivedQuantityPacks;

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
