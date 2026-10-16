package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a QC inventory record (SRS Module 5.1).
 * Includes manufacturer_id per SRS requirement (review correction C-4).
 */
@Getter
@Setter
public class QcInventoryRequest {

    @NotNull(message = "AR number is required")
    private String arNumber;

    @NotNull(message = "Manufacturer ID is required")
    private Integer manufacturerId;

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotBlank(message = "Batch number is required")
    @Size(max = 100, message = "Batch number must be at most 100 characters")
    private String batchNo;

    @NotNull(message = "Total quantity received is required")
    @Min(value = 1, message = "Total quantity received must be at least 1")
    private Integer totalQuantityReceived;

    @Size(max = 100, message = "Storage location must be at most 100 characters")
    private String storageLocation;

    private String mfgDate;

    private String expDate;
}
