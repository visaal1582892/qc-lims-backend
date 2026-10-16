package com.medplus.qc_lims_backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for external TOS update endpoint.
 *
 * Acknowledges receipt of a TOS status update callback.
 */
public record TOSUpdateResponse(
    boolean accepted,
    String message,
    Instant timestamp
) {}
