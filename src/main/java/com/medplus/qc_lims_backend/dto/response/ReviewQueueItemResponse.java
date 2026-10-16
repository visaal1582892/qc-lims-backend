package com.medplus.qc_lims_backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a review queue item.
 * Contains enough information for QC/QA Head to decide on review.
 */
public record ReviewQueueItemResponse(
    Long executionId,
    String arNumber,
    Integer parameterId,
    String parameterName,
    Integer allocatedToAnalystId,
    String executionStatus,
    String reviewStatus,
    BigDecimal resultNumericValue,
    String resultTextValue,
    String complianceFlag,
    String validityStatus,
    Instant submittedAt,
    Instant qcHeadReviewedAt,
    String reworkReason
) {
    public static ReviewQueueItemResponse fromEntity(
            com.medplus.qc_lims_backend.domain.TestExecution entity,
            String parameterName) {
        return new ReviewQueueItemResponse(
            entity.getId(),
            entity.getArNumber(),
            entity.getParameterId(),
            parameterName,
            entity.getAllocatedToAnalystId(),
            entity.getExecutionStatus(),
            entity.getReviewStatus(),
            entity.getResultNumericValue(),
            entity.getResultTextValue(),
            entity.getComplianceFlag(),
            entity.getValidityStatus(),
            entity.getSubmittedAt(),
            entity.getQcHeadReviewedAt(),
            entity.getReworkReason()
        );
    }
}
