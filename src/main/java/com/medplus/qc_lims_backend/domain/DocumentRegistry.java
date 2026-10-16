package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Document Registry entity (SRS Module 11).
 *
 * Tracks uploaded documents (ROA / COA) stored on the server filesystem.
 * Links documents to AR records or test executions.
 */
@Entity
@Table(name = "document_registry")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class DocumentRegistry extends BaseEntity {

    /** Document type: ROA or COA */
    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    /** AR number this document belongs to (nullable if linked by execution_id) */
    @Column(name = "ar_number", length = 50)
    private String arNumber;

    /** Test execution ID this document belongs to (nullable if linked by ar_number) */
    @Column(name = "execution_id")
    private Long executionId;

    /** Original file name */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Server-side file path */
    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    /** File size in bytes */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** MIME content type */
    @Column(name = "content_type", length = 100)
    private String contentType = "application/pdf";

    /** User who uploaded the document */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /** Upload timestamp */
    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    // --- Helpers ---

    /** Return the DocumentType enum for this entity's document_type. */
    public com.medplus.qc_lims_backend.enums.DocumentType getDocumentTypeEnum() {
        return com.medplus.qc_lims_backend.enums.DocumentType.fromDbValue(this.documentType);
    }

    /** Convenience: set document_type from an enum value. */
    public void setDocumentTypeEnum(com.medplus.qc_lims_backend.enums.DocumentType type) {
        this.documentType = type.getDbValue();
    }
}
