package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.DocumentRegistry;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DocumentRegistry.
 * Tracks uploaded documents (ROA / COA) on the server filesystem (SRS Module 11).
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentRegistry, Long> {

    /** Find all documents for a given AR number (active only). */
    List<DocumentRegistry> findByArNumberAndStatus(String arNumber, RecordStatus status);

    /** Find documents for a given execution ID (active only). */
    List<DocumentRegistry> findByExecutionIdAndStatus(Long executionId, RecordStatus status);

    /** Find documents by type and AR number. */
    List<DocumentRegistry> findByDocumentTypeAndArNumberAndStatus(
            String documentType, String arNumber, RecordStatus status);

    /** Find a single document by ID and active status. */
    Optional<DocumentRegistry> findByIdAndStatus(Long id, RecordStatus status);

    /** Find all active documents ordered by upload date descending. */
    List<DocumentRegistry> findAllByStatusOrderByUploadedAtDesc(RecordStatus status);

    /** Find documents for a given AR or execution. */
    @Query("SELECT d FROM DocumentRegistry d WHERE (d.arNumber = :arNumber OR d.executionId = :executionId) AND d.status = :status ORDER BY d.uploadedAt DESC")
    List<DocumentRegistry> findByArNumberOrExecutionId(
            @Param("arNumber") String arNumber,
            @Param("executionId") Long executionId,
            @Param("status") RecordStatus status);
}
