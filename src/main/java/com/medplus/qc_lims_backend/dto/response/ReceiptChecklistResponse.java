package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.TrfReceiptChecklist;
import com.medplus.qc_lims_backend.enums.RecordStatus;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Read-only response projection for TrfReceiptChecklist.
 */
public record ReceiptChecklistResponse(
    Long checklistId,
    Integer trfId,
    String arNumber,
    String matchSampleNameBatch,
    String freeFromDamageLeakage,
    String pharmacopoeialRefMentioned,
    Integer verifiedByUserId,
    LocalDateTime verifiedAt,
    RecordStatus status,
    Instant createdAt
) {
    public static ReceiptChecklistResponse fromEntity(TrfReceiptChecklist entity) {
        return new ReceiptChecklistResponse(
            entity.getId(),
            entity.getTrfId(),
            entity.getArNumber(),
            entity.getMatchSampleNameBatch(),
            entity.getFreeFromDamageLeakage(),
            entity.getPharmacopoeialRefMentioned(),
            entity.getVerifiedByUserId(),
            entity.getVerifiedAt(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
