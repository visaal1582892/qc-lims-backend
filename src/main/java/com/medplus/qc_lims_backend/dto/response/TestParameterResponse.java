package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.enums.RecordStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only response projection for TestParameterMaster.
 *
 * Uses Java record for immutability and conciseness.
 * Never exposes raw entities in API responses (architecture rule).
 */
public record TestParameterResponse(
    Long id,
    Integer productId,
    String parameterName,
    String standardReference,
    String stpDocumentNumber,
    String stpVersionNumber,
    BigDecimal minLimit,
    BigDecimal maxLimit,
    String textSpecification,
    Boolean isCritical,
    RecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static TestParameterResponse fromEntity(TestParameterMaster entity) {
        return new TestParameterResponse(
            entity.getId(),
            entity.getProductId(),
            entity.getParameterName(),
            entity.getStandardReference(),
            entity.getStpDocumentNumber(),
            entity.getStpVersionNumber(),
            entity.getMinLimit(),
            entity.getMaxLimit(),
            entity.getTextSpecification(),
            entity.getIsCritical(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
