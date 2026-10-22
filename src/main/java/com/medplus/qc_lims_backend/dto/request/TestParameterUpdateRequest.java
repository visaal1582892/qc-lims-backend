package com.medplus.qc_lims_backend.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Partial update request DTO for Test Parameter.
 *
 * All fields are optional — only non-null values are applied to the existing entity.
 * This allows the frontend to send minimal payloads (e.g., only parameterName) for updates.
 */
@Getter
@Setter
public class TestParameterUpdateRequest {

    private Integer productId;

    private String parameterName;

    private String standardReference;

    private String stpDocumentNumber;

    private String stpVersionNumber;

    private BigDecimal minLimit;

    private BigDecimal maxLimit;

    private String textSpecification;

    private Boolean isCritical;
}
