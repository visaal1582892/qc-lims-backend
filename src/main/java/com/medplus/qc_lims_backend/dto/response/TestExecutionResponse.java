package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.TestExecution;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only response projection for TestExecution.
 * Used for full detail views (single record).
 */
public record TestExecutionResponse(
    Long executionId,
    String arNumber,
    Integer parameterId,
    Integer allocatedToAnalystId,
    Integer assignedByQcHeadId,
    Integer allocatedUnits,
    Integer consumedUnits,
    String executionStatus,
    String holdOrCancelReason,
    String reworkReason,
    BigDecimal resultNumericValue,
    String resultTextValue,
    String validityStatus,
    String complianceFlag,
    String roaFilePath,
    String coaFilePath,
    Instant createdAt,
    Instant submittedAt,
    String reviewStatus,
    Instant qcHeadReviewedAt,
    Instant qaHeadReviewedAt
) {
    public static TestExecutionResponse fromEntity(TestExecution entity) {
        return new TestExecutionResponse(
            entity.getId(),
            entity.getArNumber(),
            entity.getParameterId(),
            entity.getAllocatedToAnalystId(),
            entity.getAssignedByQcHeadId(),
            entity.getAllocatedUnits(),
            entity.getConsumedUnits(),
            entity.getExecutionStatus(),
            entity.getHoldOrCancelReason(),
            entity.getReworkReason(),
            entity.getResultNumericValue(),
            entity.getResultTextValue(),
            entity.getValidityStatus(),
            entity.getComplianceFlag(),
            entity.getRoaFilePath(),
            entity.getCoaFilePath(),
            entity.getCreatedAt(),
            entity.getSubmittedAt(),
            entity.getReviewStatus(),
            entity.getQcHeadReviewedAt(),
            entity.getQaHeadReviewedAt()
        );
    }
}
