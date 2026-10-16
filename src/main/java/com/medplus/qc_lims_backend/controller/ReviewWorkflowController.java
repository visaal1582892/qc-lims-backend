package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.ReviewDecisionRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewAuditLogResponse;
import com.medplus.qc_lims_backend.dto.response.ReviewQueueItemResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.ReviewWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Multi-Tier Review, Rework, and Approval Workflow (SRS Module 7).
 *
 * Thin controller — delegates all business logic to ReviewWorkflowService.
 * All endpoints return ApiResponse<T> envelope.
 * Authorization via @PreAuthorize with Rights.java constant concatenation.
 *
 * Base path: /api/v1/reviews
 * Endpoints: 5 total (2 GET queues, 1 GET history, 2 POST decisions)
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewWorkflowController {

    private final ReviewWorkflowService service;

    // --- QC Head Review ---

    @PostMapping("/qc-head")
    @PreAuthorize("hasAuthority('\"" + Rights.ROLE_QC_HEAD_REVIEW + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> qcHeadReview(
            @RequestParam Long executionId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        var entity = service.qcHeadReview(executionId, request, null);
        return ResponseEntity.ok(ApiResponse.ok(TestExecutionResponse.fromEntity(entity)));
    }

    @GetMapping("/qc-head/queue")
    @PreAuthorize("hasAuthority('\"" + Rights.ROLE_QC_HEAD_REVIEW + "')")
    public ResponseEntity<ApiResponse<List<ReviewQueueItemResponse>>> getQcReviewQueue() {
        return ResponseEntity.ok(ApiResponse.ok(service.getQcReviewQueue()));
    }

    // --- QA Head Review ---

    @PostMapping("/qa-head")
    @PreAuthorize("hasAuthority('\"" + Rights.ROLE_QA_HEAD_REVIEW + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> qaHeadReview(
            @RequestParam Long executionId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        var entity = service.qaHeadReview(executionId, request, null);
        return ResponseEntity.ok(ApiResponse.ok(TestExecutionResponse.fromEntity(entity)));
    }

    @GetMapping("/qa-head/queue")
    @PreAuthorize("hasAuthority('\"" + Rights.ROLE_QA_HEAD_REVIEW + "')")
    public ResponseEntity<ApiResponse<List<ReviewQueueItemResponse>>> getQaReviewQueue() {
        return ResponseEntity.ok(ApiResponse.ok(service.getQaReviewQueue()));
    }

    // --- Review History ---

    @GetMapping("/{executionId}/history")
    @PreAuthorize("hasAuthority('\"" + Rights.ROLE_REVIEW_HISTORY_VIEW + "')")
    public ResponseEntity<ApiResponse<List<ReviewAuditLogResponse>>> getReviewHistory(
            @PathVariable Long executionId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getReviewHistory(executionId)));
    }
}
