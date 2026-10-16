package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating/updating a receipt checklist.
 * 3 radio fields + verified_by per SRS 3.1 D.
 */
@Getter
@Setter
public class ReceiptChecklistRequest {

    @NotNull(message = "TRF ID is required")
    private Integer trfId;

    private String arNumber;

    @NotBlank(message = "Sample name & batch match is required")
    @Pattern(regexp = "^(Yes|No|NA)$", message = "Must be Yes, No, or NA")
    private String matchSampleNameBatch;

    @NotBlank(message = "Free from damage/leakage is required")
    @Pattern(regexp = "^(Yes|No|NA)$", message = "Must be Yes, No, or NA")
    private String freeFromDamageLeakage;

    @NotBlank(message = "Pharmacopoeial reference mentioned is required")
    @Pattern(regexp = "^(Yes|No|NA)$", message = "Must be Yes, No, or NA")
    private String pharmacopoeialRefMentioned;

    @NotNull(message = "Verified by user ID is required")
    private Integer verifiedByUserId;
}
