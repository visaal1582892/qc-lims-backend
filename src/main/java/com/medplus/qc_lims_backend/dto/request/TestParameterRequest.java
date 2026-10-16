package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Unified request DTO for Test Parameter create and update operations.
 *
 * Create and Update share identical field requirements per SRS Module 1.
 * The distinction is carried by the HTTP method (POST vs PUT).
 *
 * Mirrors backend Jakarta Bean Validation for client-side form validation.
 */
@Getter
@Setter
public class TestParameterRequest {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotBlank(message = "Parameter name is required")
    @Size(min = 1, max = 150, message = "Parameter name must be 1-150 characters")
    private String parameterName;

    @NotBlank(message = "Standard reference is required")
    @Pattern(regexp = "^(IP|BP|USP|EP|JP|IS|MOA|STP)$",
             message = "Standard reference must be one of: IP, BP, USP, EP, JP, IS, MOA, STP")
    private String standardReference;

    @Size(max = 100, message = "STP document number must be at most 100 characters")
    private String stpDocumentNumber;

    @Size(max = 20, message = "STP version number must be at most 20 characters")
    private String stpVersionNumber;

    @DecimalMin(value = "0.0000", inclusive = false, message = "Min limit must be positive")
    @Digits(integer = 12, fraction = 4, message = "Min limit must have at most 12 digits and 4 decimals")
    private BigDecimal minLimit;

    @DecimalMin(value = "0.0000", inclusive = false, message = "Max limit must be positive")
    @Digits(integer = 12, fraction = 4, message = "Max limit must have at most 12 digits and 4 decimals")
    private BigDecimal maxLimit;

    private String textSpecification;

    private Boolean isCritical;
}
