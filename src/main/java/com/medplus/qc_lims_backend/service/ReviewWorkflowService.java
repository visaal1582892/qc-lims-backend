package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.ReviewAuditLog;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.dto.request.ReviewDecisionRequest;
import com.medplus.qc_lims_backend.dto.response.ReviewAuditLogResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewQueueItemResponse;
import com.medplus.qc_lims_backend.enums.ExecutionStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.enums.ReviewStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.ReviewAuditLogRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Multi-Tier Review, Rework, and Approval Workflow (SRS Module 7).
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
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewWorkflowService {

    private static final String DECISION_APPROVE = "APPROVE";
    private static final String DECISION_REWORK  = "REWORK";

    private final TestExecutionRepository testExecutionRepository;
    private final ReviewAuditLogRepository auditLogRepository;
    private final TestParameterMasterRepository parameterRepository;

    // --- QC Head Review ---

    @Transactional
    public TestExecution qcHeadReview(Long executionId, ReviewDecisionRequest request, Long qcHeadId) {
        TestExecution entity = loadActive(executionId);

        // Validate: must be Under_Review with PENDING_QC
        if (!ExecutionStatus.UNDER_REVIEW.getDbValue().equals(entity.getExecutionStatus())) {
            throw new BusinessRuleViolationException(
                    "Cannot review: execution is in status '" + entity.getExecutionStatus()
                    + "', expected 'Under_Review'.");
        }
        if (!ReviewStatus.PENDING_QC.getDbValue().equals(entity.getReviewStatus())) {
            throw new BusinessRuleViolationException(
                    "Cannot review: review_status is '" + entity.getReviewStatus()
                    + "', expected 'PENDING_QC'.");
        }

        String decision = request.getDecision();
        String reason   = request.getReason();

        if (DECISION_REWORK.equals(decision)) {
            // BR: Mandatory reason for rework
            if (reason == null || reason.isBlank()) {
                throw new BusinessRuleViolationException("Rework reason is mandatory.");
            }
            return qcHeadRework(entity, reason, qcHeadId);
        }

        if (DECISION_APPROVE.equals(decision)) {
            return qcHeadApprove(entity, qcHeadId);
        }

        throw new BusinessRuleViolationException("Invalid decision: '" + decision + "'. Must be APPROVE or REWORK.");
    }

    private TestExecution qcHeadApprove(TestExecution entity, Long qcHeadId) {
        entity.setExecutionStatus(ExecutionStatus.RELEASED.getDbValue());
        entity.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        entity.setQcHeadReviewedAt(Instant.now());
        entity = testExecutionRepository.save(entity);

        auditLogRepository.save(buildAuditLog(entity.getId(), "QC_HEAD", qcHeadId, DECISION_APPROVE, null));

        log.info("QC Head approved: executionId={}", entity.getId());
        return entity;
    }

    private TestExecution qcHeadRework(TestExecution entity, String reason, Long qcHeadId) {
        entity.setExecutionStatus(ExecutionStatus.REWORK_REQUESTED.getDbValue());
        entity.setReviewStatus(ReviewStatus.REWORK_REQUESTED.getDbValue());

        // Append rework reason with timestamp
        String existing = entity.getReworkReason();
        String entry = String.format("[QC Rework requested on %s: %s]", Instant.now(), reason);
        entity.setReworkReason(existing != null && !existing.isBlank()
                ? existing + "\n" + entry : entry);

        entity = testExecutionRepository.save(entity);

        auditLogRepository.save(buildAuditLog(entity.getId(), "QC_HEAD", qcHeadId, DECISION_REWORK, reason));

        log.info("QC Head requested rework: executionId={}, reason={}", entity.getId(), reason);
        return entity;
    }

    // --- QA Head Review ---

    @Transactional
    public TestExecution qaHeadReview(Long executionId, ReviewDecisionRequest request, Long qaHeadId) {
        TestExecution entity = loadActive(executionId);

        // Validate: must be Released with PENDING_QA
        if (!ExecutionStatus.RELEASED.getDbValue().equals(entity.getExecutionStatus())) {
            throw new BusinessRuleViolationException(
                    "Cannot review: execution is in status '" + entity.getExecutionStatus()
                    + "', expected 'Released'.");
        }
        if (!ReviewStatus.PENDING_QA.getDbValue().equals(entity.getReviewStatus())) {
            throw new BusinessRuleViolationException(
                    "Cannot review: review_status is '" + entity.getReviewStatus()
                    + "', expected 'PENDING_QA'.");
        }

        String decision = request.getDecision();
        String reason   = request.getReason();

        if (DECISION_REWORK.equals(decision)) {
            // BR: Mandatory reason for rework
            if (reason == null || reason.isBlank()) {
                throw new BusinessRuleViolationException("Rework reason is mandatory.");
            }
            return qaHeadRework(entity, reason, qaHeadId);
        }

        if (DECISION_APPROVE.equals(decision)) {
            return qaHeadApprove(entity, qaHeadId);
        }

        throw new BusinessRuleViolationException("Invalid decision: '" + decision + "'. Must be APPROVE or REWORK.");
    }

    private TestExecution qaHeadApprove(TestExecution entity, Long qaHeadId) {
        entity.setReviewStatus(ReviewStatus.QC_CLEARED.getDbValue());
        entity.setQaHeadReviewedAt(Instant.now());
        entity = testExecutionRepository.save(entity);

        auditLogRepository.save(buildAuditLog(entity.getId(), "QA_HEAD", qaHeadId, DECISION_APPROVE, null));

        log.info("QA Head approved (QC Cleared): executionId={}", entity.getId());
        return entity;
    }

    private TestExecution qaHeadRework(TestExecution entity, String reason, Long qaHeadId) {
        entity.setExecutionStatus(ExecutionStatus.REWORK_REQUESTED.getDbValue());
        entity.setReviewStatus(ReviewStatus.REWORK_REQUESTED.getDbValue());

        // Append rework reason with timestamp
        String existing = entity.getReworkReason();
        String entry = String.format("[QA Rework requested on %s: %s]", Instant.now(), reason);
        entity.setReworkReason(existing != null && !existing.isBlank()
                ? existing + "\n" + entry : entry);

        entity = testExecutionRepository.save(entity);

        auditLogRepository.save(buildAuditLog(entity.getId(), "QA_HEAD", qaHeadId, DECISION_REWORK, reason));

        log.info("QA Head requested rework: executionId={}, reason={}", entity.getId(), reason);
        return entity;
    }

    // --- Review Queues ---

    /**
     * Get QC Head review queue: executions in Under_Review with PENDING_QC review status.
     */
    public List<ReviewQueueItemResponse> getQcReviewQueue() {
        List<TestExecution> executions = testExecutionRepository.findByExecutionStatusAndReviewStatusAndStatus(
                ExecutionStatus.UNDER_REVIEW.getDbValue(),
                ReviewStatus.PENDING_QC.getDbValue(),
                RecordStatus.ACTIVE);
        return mapToQueueList(executions);
    }

    /**
     * Get QA Head review queue: executions in Released with PENDING_QA review status.
     */
    public List<ReviewQueueItemResponse> getQaReviewQueue() {
        List<TestExecution> executions = testExecutionRepository.findByExecutionStatusInAndReviewStatusAndStatus(
                List.of(ExecutionStatus.RELEASED.getDbValue()),
                ReviewStatus.PENDING_QA.getDbValue(),
                RecordStatus.ACTIVE);
        return mapToQueueList(executions);
    }

    /**
     * Get full review history (audit trail) for an execution.
     */
    public List<ReviewAuditLogResponse> getReviewHistory(Long executionId) {
        // Verify execution exists
        loadActive(executionId);

        List<ReviewAuditLog> logs = auditLogRepository.findByExecutionIdOrderByExecutedAtAsc(executionId);
        return logs.stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    private TestExecution loadActive(Long id) {
        return testExecutionRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestExecution", id));
    }

    private ReviewAuditLog buildAuditLog(Long executionId, String stage, Long reviewerId,
                                         String decision, String reason) {
        ReviewAuditLog log = new ReviewAuditLog();
        log.setExecutionId(executionId);
        log.setReviewStage(stage);
        log.setReviewerId(reviewerId);
        log.setDecision(decision);
        log.setReason(reason);
        log.setExecutedAt(Instant.now());
        log.setStatus(RecordStatus.ACTIVE);
        return log;
    }

    private ReviewAuditLogResponse toAuditLogResponse(ReviewAuditLog log) {
        return new ReviewAuditLogResponse(
                log.getId(),
                log.getExecutionId(),
                log.getReviewStage(),
                log.getReviewerId(),
                log.getDecision(),
                log.getReason(),
                log.getExecutedAt()
        );
    }

    /**
     * Map executions to queue item responses with parameter names resolved.
     */
    private List<ReviewQueueItemResponse> mapToQueueList(List<TestExecution> executions) {
        if (executions.isEmpty()) return List.of();

        // Batch load parameter names
        java.util.Set<Integer> paramIds = executions.stream()
                .map(TestExecution::getParameterId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> paramNameMap = parameterRepository
                .findByIdInAndStatus(paramIds.stream().map(Long::valueOf).collect(Collectors.toList()), RecordStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getId().intValue(),
                        TestParameterMaster::getParameterName,
                        (a, b) -> a
                ));

        return executions.stream()
                .map(entity -> ReviewQueueItemResponse.fromEntity(
                        entity,
                        paramNameMap.getOrDefault(entity.getParameterId(), "Unknown")))
                .collect(Collectors.toList());
    }
}
