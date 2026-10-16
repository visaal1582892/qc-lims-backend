package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Request DTO for external TOS update endpoint.
 *
 * Standard callback payload for receiving TOS status updates from
 * an external warehouse/ERP system. Accepts either grnNumber or
 * trfId for identification.
 */
public record TOSUpdateRequest(
    @Size(max = 100)
    String grnNumber,

    Long trfId,

    @NotBlank(message = "Status is required")
    String status,

    Map<String, Object> updatedFields,

    String callbackTimestamp,

    @Size(max = 500)
    String remarks
) {}
