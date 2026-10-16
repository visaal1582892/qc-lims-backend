package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.TestExecution;

import java.time.Instant;

/**
 * Summary projection for list views and dashboard (SRS Module 6).
 * Lightweight — excludes large text fields (reasons, file paths).
 */
public record TestExecutionSummaryResponse(
    Long executionId,
    String arNumber,
    Integer parameterId,
    String parameterName,
    Integer allocatedToAnalystId,
    String executionStatus,
    String complianceFlag,
    Integer allocatedUnits,
    Integer consumedUnits,
    Instant createdAt,
    Instant submittedAt
) {
    public static TestExecutionSummaryResponse fromEntity(TestExecution entity, String parameterName) {
        return new TestExecutionSummaryResponse(
            entity.getId(),
            entity.getArNumber(),
            entity.getParameterId(),
            parameterName,
            entity.getAllocatedToAnalystId(),
            entity.getExecutionStatus(),
            entity.getComplianceFlag(),
            entity.getAllocatedUnits(),
            entity.getConsumedUnits(),
            entity.getCreatedAt(),
            entity.getSubmittedAt()
        );
    }
}
