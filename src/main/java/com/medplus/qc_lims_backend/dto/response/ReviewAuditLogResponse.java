package com.medplus.qc_lims_backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for a single review audit log entry.
 * Returned in review history timeline.
 */
public record ReviewAuditLogResponse(
    Long logId,
    Long executionId,
    String reviewStage,
    Long reviewerId,
    String decision,
    String reason,
    Instant executedAt
) {}
