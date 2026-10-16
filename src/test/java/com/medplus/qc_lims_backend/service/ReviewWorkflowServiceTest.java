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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ReviewWorkflowService — Multi-Tier Review, Rework & Approval.
 *
 * Test matrix:
 *   R-01: qcHeadReview APPROVE — moves to PENDING_QA
 *   R-02: qcHeadReview REWORK — returns Rework_Requested with mandatory reason
 *   R-03: qcHeadReview REWORK without reason — rejected
 *   R-04: qcHeadReview on wrong execution status — rejected
 *   R-05: qcHeadReview on wrong review status — rejected
 *   R-06: qcHeadReview invalid decision — rejected
 *   R-07: qaHeadReview APPROVE — moves to QC_CLEARED
 *   R-08: qaHeadReview REWORK — returns Rework_Requested with mandatory reason
 *   R-09: qaHeadReview REWORK without reason — rejected
 *   R-10: qaHeadReview on wrong execution status — rejected
 *   R-11: getQcReviewQueue returns PENDING_QC items
 *   R-12: getQaReviewQueue returns PENDING_QA items
 *   R-13: getReviewHistory returns ordered audit log
 *   R-14: getReviewHistory for non-existent execution — 404
 *   R-15: Rework under same AR number (no new AR generated)
 */
@ExtendWith(MockitoExtension.class)
class ReviewWorkflowServiceTest {

    @Mock private TestExecutionRepository testExecutionRepository;
    @Mock private ReviewAuditLogRepository auditLogRepository;
    @Mock private TestParameterMasterRepository parameterRepository;

    @InjectMocks private ReviewWorkflowService service;

    private TestExecution execution;

    @BeforeEach
    void setUp() {
        execution = new TestExecution();
        execution.setId(1L);
        execution.setArNumber("AR-001");
        execution.setParameterId(1);
        execution.setAllocatedToAnalystId(10);
        execution.setAssignedByQcHeadId(1);
        execution.setAllocatedUnits(5);
        execution.setConsumedUnits(0);
        execution.setResultNumericValue(new BigDecimal("99.5"));
        execution.setComplianceFlag("PASS");
        execution.setValidityStatus("Valid");
        execution.setCreatedAt(Instant.now());
        execution.setSubmittedAt(Instant.now());
        execution.setStatus(RecordStatus.ACTIVE);
    }

    // --- R-01: QC Head Approve ---

    @Test
    void r01QcHeadReviewApproveMovesToPendingQa() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        service.qcHeadReview(1L, request, 100L);

        assertEquals(ExecutionStatus.RELEASED.getDbValue(), execution.getExecutionStatus());
        assertEquals(ReviewStatus.PENDING_QA.getDbValue(), execution.getReviewStatus());
        assertNotNull(execution.getQcHeadReviewedAt());

        ArgumentCaptor<ReviewAuditLog> logCaptor = ArgumentCaptor.forClass(ReviewAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        ReviewAuditLog savedLog = logCaptor.getValue();
        assertEquals("QC_HEAD", savedLog.getReviewStage());
        assertEquals("APPROVE", savedLog.getDecision());
        assertEquals(100L, savedLog.getReviewerId());
    }

    // --- R-02: QC Head Rework ---

    @Test
    void r02QcHeadReviewReworkReturnsToAnalyst() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("REWORK");
        request.setReason("Results need verification");

        TestExecution result = service.qcHeadReview(1L, request, 100L);

        assertEquals(ExecutionStatus.REWORK_REQUESTED.getDbValue(), execution.getExecutionStatus());
        assertEquals(ReviewStatus.REWORK_REQUESTED.getDbValue(), execution.getReviewStatus());
        assertEquals("AR-001", result.getArNumber()); // Same AR number preserved
        assertNotNull(execution.getReworkReason());
        assertTrue(execution.getReworkReason().contains("Results need verification"));

        ArgumentCaptor<ReviewAuditLog> logCaptor = ArgumentCaptor.forClass(ReviewAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertEquals("REWORK", logCaptor.getValue().getDecision());
    }

    // --- R-03: QC Head Rework without reason ---

    @Test
    void r03QcHeadReviewReworkWithoutReasonRejected() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("REWORK");
        request.setReason(null);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qcHeadReview(1L, request, 100L));
    }

    // --- R-04: QC Head Review on wrong execution status ---

    @Test
    void r04QcHeadReviewWrongExecutionStatusRejected() {
        execution.setExecutionStatus(ExecutionStatus.ALLOCATED.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qcHeadReview(1L, request, 100L));
    }

    // --- R-05: QC Head Review on wrong review status ---

    @Test
    void r05QcHeadReviewWrongReviewStatusRejected() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qcHeadReview(1L, request, 100L));
    }

    // --- R-06: QC Head Review invalid decision ---

    @Test
    void r06QcHeadReviewInvalidDecisionRejected() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("INVALID");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qcHeadReview(1L, request, 100L));
    }

    // --- R-07: QA Head Approve ---

    @Test
    void r07QaHeadReviewApproveMovesToQcCleared() {
        execution.setExecutionStatus(ExecutionStatus.RELEASED.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        service.qaHeadReview(1L, request, 200L);

        assertEquals(ReviewStatus.QC_CLEARED.getDbValue(), execution.getReviewStatus());
        assertNotNull(execution.getQaHeadReviewedAt());

        ArgumentCaptor<ReviewAuditLog> logCaptor = ArgumentCaptor.forClass(ReviewAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertEquals("QA_HEAD", logCaptor.getValue().getReviewStage());
        assertEquals("APPROVE", logCaptor.getValue().getDecision());
    }

    // --- R-08: QA Head Rework ---

    @Test
    void r08QaHeadReviewReworkReturnsToAnalyst() {
        execution.setExecutionStatus(ExecutionStatus.RELEASED.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("REWORK");
        request.setReason("Non-conforming documentation");

        TestExecution result = service.qaHeadReview(1L, request, 200L);

        assertEquals(ExecutionStatus.REWORK_REQUESTED.getDbValue(), execution.getExecutionStatus());
        assertEquals(ReviewStatus.REWORK_REQUESTED.getDbValue(), execution.getReviewStatus());
        assertEquals("AR-001", result.getArNumber());
        assertNotNull(execution.getReworkReason());
        assertTrue(execution.getReworkReason().contains("Non-conforming documentation"));
    }

    // --- R-09: QA Head Rework without reason ---

    @Test
    void r09QaHeadReviewReworkWithoutReasonRejected() {
        execution.setExecutionStatus(ExecutionStatus.RELEASED.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("REWORK");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qaHeadReview(1L, request, 200L));
    }

    // --- R-10: QA Head Review on wrong status ---

    @Test
    void r10QaHeadReviewWrongExecutionStatusRejected() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QA.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.qaHeadReview(1L, request, 200L));
    }

    // --- R-11: QC Review Queue ---

    @Test
    void r11GetQcReviewQueueReturnsPendingQcItems() {
        TestExecution e = buildExecution(ExecutionStatus.UNDER_REVIEW, ReviewStatus.PENDING_QC);
        when(testExecutionRepository.findByExecutionStatusAndReviewStatusAndStatus(
                eq(ExecutionStatus.UNDER_REVIEW.getDbValue()),
                eq(ReviewStatus.PENDING_QC.getDbValue()),
                eq(RecordStatus.ACTIVE))).thenReturn(List.of(e));

        TestParameterMaster param = new TestParameterMaster();
        param.setId(1L);
        param.setParameterName("Assay");
        param.setStatus(RecordStatus.ACTIVE);
        when(parameterRepository.findByIdInAndStatus(anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(List.of(param));

        List<ReviewQueueItemResponse> queue = service.getQcReviewQueue();

        assertEquals(1, queue.size());
        assertEquals(1L, queue.get(0).executionId());
        assertEquals("Assay", queue.get(0).parameterName());
    }

    // --- R-12: QA Review Queue ---

    @Test
    void r12GetQaReviewQueueReturnsPendingQaItems() {
        TestExecution e = buildExecution(ExecutionStatus.RELEASED, ReviewStatus.PENDING_QA);
        when(testExecutionRepository.findByExecutionStatusInAndReviewStatusAndStatus(
                anyList(), eq(ReviewStatus.PENDING_QA.getDbValue()), eq(RecordStatus.ACTIVE)))
                .thenReturn(List.of(e));

        TestParameterMaster param = new TestParameterMaster();
        param.setId(1L);
        param.setParameterName("Microbial");
        param.setStatus(RecordStatus.ACTIVE);
        when(parameterRepository.findByIdInAndStatus(anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(List.of(param));

        List<ReviewQueueItemResponse> queue = service.getQaReviewQueue();

        assertEquals(1, queue.size());
        assertEquals("Microbial", queue.get(0).parameterName());
    }

    // --- R-13: Review History ---

    @Test
    void r13GetReviewHistoryReturnsOrderedAuditLog() {
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        ReviewAuditLog log1 = new ReviewAuditLog();
        log1.setId(1L);
        log1.setExecutionId(1L);
        log1.setReviewStage("QC_HEAD");
        log1.setReviewerId(100L);
        log1.setDecision("APPROVE");
        log1.setExecutedAt(Instant.parse("2026-01-01T10:00:00Z"));

        ReviewAuditLog log2 = new ReviewAuditLog();
        log2.setId(2L);
        log2.setExecutionId(1L);
        log2.setReviewStage("QA_HEAD");
        log2.setReviewerId(200L);
        log2.setDecision("APPROVE");
        log2.setExecutedAt(Instant.parse("2026-01-01T11:00:00Z"));

        when(auditLogRepository.findByExecutionIdOrderByExecutedAtAsc(1L))
                .thenReturn(List.of(log1, log2));

        List<ReviewAuditLogResponse> history = service.getReviewHistory(1L);

        assertEquals(2, history.size());
        assertEquals("QC_HEAD", history.get(0).reviewStage());
        assertEquals("QA_HEAD", history.get(1).reviewStage());
    }

    // --- R-14: Review History for non-existent execution ---

    @Test
    void r14GetReviewHistoryNonExistentRejected() {
        when(testExecutionRepository.findByIdAndStatus(999L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.getReviewHistory(999L));
    }

    // --- R-15: Rework preserves AR number ---

    @Test
    void r15ReworkPreservesSameArNumber() {
        execution.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        execution.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE)).thenReturn(Optional.of(execution));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(i -> i.getArgument(0));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("REWORK");
        request.setReason("Incorrect calculation");

        TestExecution result = service.qcHeadReview(1L, request, 100L);

        assertEquals("AR-001", result.getArNumber());
    }

    // --- Helper ---

    private TestExecution buildExecution(ExecutionStatus execStatus, ReviewStatus revStatus) {
        TestExecution e = new TestExecution();
        e.setId(1L);
        e.setArNumber("AR-001");
        e.setParameterId(1);
        e.setAllocatedToAnalystId(10);
        e.setExecutionStatus(execStatus.getDbValue());
        e.setReviewStatus(revStatus.getDbValue());
        e.setComplianceFlag("PASS");
        e.setValidityStatus("Valid");
        e.setSubmittedAt(Instant.now());
        e.setStatus(RecordStatus.ACTIVE);
        return e;
    }
}
