package com.medplus.qc_lims_backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for external TOS trigger endpoint.
 *
 * Acknowledges receipt of a TOS trigger payload. The trfId field is
 * reserved for when actual e-TRF auto-creation logic is implemented.
 */
public record TOSTriggerResponse(
    boolean accepted,
    Long trfId,
    String message,
    Instant timestamp
) {}
