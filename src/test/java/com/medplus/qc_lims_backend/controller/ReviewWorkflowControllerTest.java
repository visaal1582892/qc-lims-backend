package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.dto.request.ReviewDecisionRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewAuditLogResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewQueueItemResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.service.ReviewWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ReviewWorkflowController — verifies thin controller delegates to service
 * and returns correct ApiResponse envelopes.
 *
 * R-20: QC Head review POST returns 200 with ApiResponse
 * R-21: QA Head review POST returns 200 with ApiResponse
 * R-22: QC Head queue GET returns 200
 * R-23: QA Head queue GET returns 200
 * R-24: Review history GET returns 200
 */
class ReviewWorkflowControllerTest {

    private final ReviewWorkflowService service = mock(ReviewWorkflowService.class);
    private final ReviewWorkflowController controller = new ReviewWorkflowController(service);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private TestExecution makeEntity() {
        TestExecution e = new TestExecution();
        e.setId(1L);
        e.setArNumber("AR-001");
        e.setParameterId(1);
        e.setAllocatedToAnalystId(10);
        e.setAssignedByQcHeadId(1);
        e.setAllocatedUnits(5);
        e.setConsumedUnits(0);
        e.setExecutionStatus("Under_Review");
        e.setComplianceFlag("PASS");
        e.setValidityStatus("Valid");
        e.setCreatedAt(NOW);
        e.setSubmittedAt(NOW);
        e.setReviewStatus("PENDING_QC");
        return e;
    }

    private ReviewQueueItemResponse makeQueueItem() {
        return new ReviewQueueItemResponse(
                1L, "AR-001", 1, "Assay", 10,
                "Under_Review", "PENDING_QC",
                new BigDecimal("99.5"), null, "PASS", "Valid",
                NOW, null, null);
    }

    private ReviewAuditLogResponse makeAuditLog() {
        return new ReviewAuditLogResponse(
                1L, 1L, "QC_HEAD", 100L, "APPROVE", null, NOW);
    }

    // --- R-20: QC Head review ---

    @Test
    void r20QcHeadReviewReturns200WithApiResponse() {
        TestExecution entity = makeEntity();
        when(service.qcHeadReview(eq(1L), any(ReviewDecisionRequest.class), any())).thenReturn(entity);

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.qcHeadReview(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<TestExecutionResponse> body = response.getBody();
        assertTrue(body.success());
        assertNotNull(body.data());
        assertEquals("AR-001", body.data().arNumber());
    }

    // --- R-21: QA Head review ---

    @Test
    void r21QaHeadReviewReturns200WithApiResponse() {
        TestExecution entity = makeEntity();
        when(service.qaHeadReview(eq(1L), any(ReviewDecisionRequest.class), any())).thenReturn(entity);

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setDecision("APPROVE");

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.qaHeadReview(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
    }

    // --- R-22: QC Head queue ---

    @Test
    void r22GetQcReviewQueueReturns200() {
        when(service.getQcReviewQueue()).thenReturn(List.of(makeQueueItem()));

        ResponseEntity<ApiResponse<List<ReviewQueueItemResponse>>> response = controller.getQcReviewQueue();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    // --- R-23: QA Head queue ---

    @Test
    void r23GetQaReviewQueueReturns200() {
        when(service.getQaReviewQueue()).thenReturn(List.of(makeQueueItem()));

        ResponseEntity<ApiResponse<List<ReviewQueueItemResponse>>> response = controller.getQaReviewQueue();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    // --- R-24: Review history ---

    @Test
    void r24GetReviewHistoryReturns200() {
        when(service.getReviewHistory(1L)).thenReturn(List.of(makeAuditLog()));

        ResponseEntity<ApiResponse<List<ReviewAuditLogResponse>>> response = controller.getReviewHistory(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
        assertEquals("QC_HEAD", response.getBody().data().get(0).reviewStage());
    }
}
