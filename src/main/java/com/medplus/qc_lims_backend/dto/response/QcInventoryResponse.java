package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.QcInventory;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Read-only response projection for QcInventory.
 * Includes computed variance field.
 */
public record QcInventoryResponse(
    Long inventoryId,
    String arNumber,
    Integer manufacturerId,
    Integer productId,
    String batchNo,
    Integer totalQuantityReceived,
    Integer quantityAllocated,
    Integer quantityConsumed,
    Integer currentBalance,
    Integer variance,
    String storageLocation,
    LocalDate mfgDate,
    LocalDate expDate,
    Long receivedByUserId,
    String status,
    Instant createdAt
) {
    public static QcInventoryResponse fromEntity(QcInventory entity) {
        return new QcInventoryResponse(
            entity.getId(),
            entity.getArNumber(),
            entity.getManufacturerId(),
            entity.getProductId(),
            entity.getBatchNo(),
            entity.getTotalQuantityReceived(),
            entity.getQuantityAllocated(),
            entity.getQuantityConsumed(),
            entity.getCurrentBalance(),
            entity.getVariance(),
            entity.getStorageLocation(),
            entity.getMfgDate(),
            entity.getExpDate(),
            entity.getCreatedBy(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getCreatedAt()
        );
    }
}
