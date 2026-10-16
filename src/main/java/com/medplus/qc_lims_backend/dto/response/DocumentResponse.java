package com.medplus.qc_lims_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medplus.qc_lims_backend.domain.DocumentRegistry;

import java.time.Instant;

/**
 * Response DTO for DocumentRegistry records (SRS Module 11).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
        Long id,
        String documentType,
        String arNumber,
        Long executionId,
        String fileName,
        Long fileSizeBytes,
        String contentType,
        Instant uploadedAt,
        Long uploadedBy
) {
    public static DocumentResponse fromEntity(DocumentRegistry entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getDocumentType(),
                entity.getArNumber(),
                entity.getExecutionId(),
                entity.getFileName(),
                entity.getFileSizeBytes(),
                entity.getContentType(),
                entity.getUploadedAt(),
                entity.getUploadedBy()
        );
    }
}
