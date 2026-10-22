package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.ReviewAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for review_audit_log — audit trail of review decisions.
 */
@Repository
public interface ReviewAuditLogRepository extends JpaRepository<ReviewAuditLog, Long> {

    /**
     * Find all audit log entries for a given execution, ordered chronologically.
     */
    List<ReviewAuditLog> findByExecutionIdOrderByExecutedAtAsc(Long executionId);

    /**
     * Find all audit log entries by status, ordered by executed_at descending.
     */
    List<ReviewAuditLog> findAllByStatusOrderByExecutedAtDesc(com.medplus.qc_lims_backend.enums.RecordStatus status);
}
