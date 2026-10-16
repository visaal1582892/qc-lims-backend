package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.enums.RecordStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Read-only response projection for SampleInwardRegister.
 */
public record InwardResponse(
    Long id,
    String arNumber,
    Integer trfId,
    Integer productId,
    String batchNo,
    Integer receivedQuantityPacks,
    Integer totalReceivedUnits,
    String storageLocation,
    String arStatus,
    Boolean isSubcontracted,
    String subcontractRemarks,
    LocalDate receiptDate,
    RecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static InwardResponse fromEntity(SampleInwardRegister entity) {
        return new InwardResponse(
            entity.getId(),
            entity.getArNumber(),
            entity.getTrfId(),
            entity.getProductId(),
            entity.getBatchNo(),
            entity.getReceivedQuantityPacks(),
            entity.getTotalReceivedUnits(),
            entity.getStorageLocation(),
            entity.getArStatus(),
            entity.getIsSubcontracted(),
            entity.getSubcontractRemarks(),
            entity.getReceiptDate(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
