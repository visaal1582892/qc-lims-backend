package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.OosIncidentRegister;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Response DTO for OOS incident records.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OosIncidentResponse(
        Long id,
        String incidentNumber,
        String arNumber,
        Integer executionId,
        String productName,
        Integer productId,
        String batchNo,
        String testCode,
        String specificationReference,
        String observedValue,
        String unitOfMeasurement,
        String deviationDescription,
        String testMethodName,
        String oosStatus,
        String finalDisposition,
        String investigationSummary,
        Integer retestAttemptCount,
        String rootCause,
        String correctiveAction,
        String revisedResult,
        String revisedResultUnit,
        LocalDateTime retestCompletedDate,
        Long retestIncidentId,
        Integer raisedByUserId,
        Integer qcHeadAuthorizedId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** Build response directly from entity — all fields are now on the entity. */
    public static OosIncidentResponse fromEntity(OosIncidentRegister entity) {
        return new OosIncidentResponse(
                entity.getId(),
                entity.getIncidentNumber(),
                entity.getArNumber(),
                entity.getExecutionId(),
                entity.getProductName(),
                entity.getProductId(),
                entity.getBatchNo(),
                entity.getTestCode(),
                entity.getSpecificationReference(),
                entity.getObservedValue(),
                entity.getUnitOfMeasurement(),
                entity.getDeviationDescription(),
                entity.getTestMethodName(),
                entity.getOosStatus(),
                entity.getFinalDisposition(),
                entity.getInvestigationSummary(),
                entity.getRetestAttemptCount(),
                entity.getRootCause(),
                entity.getCorrectiveAction(),
                entity.getRevisedResult(),
                entity.getRevisedResultUnit(),
                entity.getRetestCompletedDate(),
                entity.getRetestIncidentId(),
                entity.getRaisedByUserId(),
                entity.getQcHeadAuthorizedId(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}
