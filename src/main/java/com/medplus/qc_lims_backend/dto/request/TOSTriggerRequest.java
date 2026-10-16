package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for external TOS trigger endpoint.
 *
 * Standard payload for receiving GRN/Transfer Order from an external
 * warehouse/ERP system. Per SRS Module 14.1 — fields cover everything
 * needed to auto-create an e-TRF in the future.
 */
public record TOSTriggerRequest(
    @NotBlank(message = "GRN number is required")
    @Size(max = 100)
    String grnNumber,

    @NotBlank(message = "Transfer order number is required")
    @Size(max = 100)
    String transferOrderNumber,

    @NotNull(message = "Manufacturer ID is required")
    Integer manufacturerId,

    @NotNull(message = "Product ID is required")
    Integer productId,

    @NotBlank(message = "Batch numbers are required")
    String batchNumbers,

    @Size(max = 100)
    String materialType,

    String mfgDate,

    String expDate,

    Integer sampleQty,

    @Size(max = 50)
    String storageCondition,

    List<String> requestedTestParameters,

    @Size(max = 255)
    String companyName,

    @Size(max = 150)
    String contactPersonName,

    @Size(max = 50)
    String phoneMobile,

    String remarks
) {}
