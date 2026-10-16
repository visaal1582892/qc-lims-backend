package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for generating a barcode with product name override.
 */
@Getter
@Setter
public class BarcodeRequest {

    /** B-2/M-3 fix: productName is optional — GET endpoint passes null, POST may omit it */
    @Size(max = 200, message = "Product name must be at most 200 characters")
    private String productName;
}
