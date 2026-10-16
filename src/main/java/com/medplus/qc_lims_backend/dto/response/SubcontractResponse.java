package com.medplus.qc_lims_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medplus.qc_lims_backend.domain.SubcontractRegister;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for SubcontractRegister records.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubcontractResponse(
        Long id,
        String subcontractNo,
        Integer trfId,
        String arNumber,
        String externalLabName,
        String reason,
        LocalDate dispatchDate,
        Long dispatchedByQcHeadId,
        LocalDate expectedReturnDate,
        String status,
        String remarks,
        Boolean roaReceived,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubcontractResponse fromEntity(SubcontractRegister entity) {
        return new SubcontractResponse(
                entity.getId(),
                entity.getSubcontractNo(),
                entity.getTrfId(),
                entity.getArNumber(),
                entity.getExternalLabName(),
                entity.getReason(),
                entity.getDispatchDate(),
                entity.getDispatchedByQcHeadId(),
                entity.getExpectedReturnDate(),
                entity.getSubcontractStatus(),
                entity.getRemarks(),
                entity.getRoaReceived(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
