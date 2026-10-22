package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.dto.request.ReviewDecisionRequest;
import com.medplus.qc_lims_backend.dto.response.ReviewAuditLogResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewQueueItemResponse;

import java.util.List;

/**
 * Service layer interface for Multi-Tier Review, Rework, and Approval Workflow (SRS Module 7).
 *
 * Flow:
 *   Analyst submits result -> Under_Review (review_status = PENDING_QC)
 *   QC Head reviews:
 *     APPROVE -> Released (review_status = PENDING_QA, move to QA queue)
 *     REWORK  -> Rework_Requested (review_status = REWORK_REQUESTED, same AR)
 *   QA Head reviews:
 *     APPROVE -> Released (review_status = QC_CLEARED, COA locked)
 *     REWORK  -> Rework_Requested (review_status = REWORK_REQUESTED, same AR)
 *
 * All review decisions are logged as immutable audit trail entries.
 */
public interface ReviewWorkflowService {

    // --- QC Head Review ---

    /**
     * QC Head review of an execution.
     * @param executionId the test execution ID
     * @param request the review decision (APPROVE or REWORK)
     * @param qcHeadId the QC Head's user ID
     * @return the updated TestExecution
     */
    TestExecution qcHeadReview(Long executionId, ReviewDecisionRequest request, Long qcHeadId);

    // --- QA Head Review ---

    /**
     * QA Head review of an execution.
     * @param executionId the test execution ID
     * @param request the review decision (APPROVE or REWORK)
     * @param qaHeadId the QA Head's user ID
     * @return the updated TestExecution
     */
    TestExecution qaHeadReview(Long executionId, ReviewDecisionRequest request, Long qaHeadId);

    // --- Review Queues ---

    /**
     * Get QC Head review queue: executions in Under_Review with PENDING_QC review status.
     */
    List<ReviewQueueItemResponse> getQcReviewQueue();

    /**
     * Get QA Head review queue: executions in Released with PENDING_QA review status.
     */
    List<ReviewQueueItemResponse> getQaReviewQueue();

    /**
     * Get full review history (audit trail) for an execution.
     */
    List<ReviewAuditLogResponse> getReviewHistory(Long executionId);

    /**
     * Get all audit log entries (across all executions) — for global audit trail view.
     */
    List<ReviewAuditLogResponse> getAuditLog();
}
