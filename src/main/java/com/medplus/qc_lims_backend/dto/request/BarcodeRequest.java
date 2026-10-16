package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for generating a barcode with product name override.
 */
@Getter
@Setter
public class BarcodeRequest {

    @NotBlank(message = "Product name is required for barcode generation")
    private String productName;
}
