package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.TestAllocationRequest;
import com.medplus.qc_lims_backend.dto.request.TestReassignmentRequest;
import com.medplus.qc_lims_backend.dto.request.TestResultSubmissionRequest;
import com.medplus.qc_lims_backend.dto.request.TestStatusUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionSummaryResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TestExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Test Allocation & Execution State Machine (SRS Module 6).
 *
 * Thin controller — delegates all business logic to TestExecutionService.
 * All endpoints return ApiResponse<T> envelope.
 * Authorization via @PreAuthorize with Rights.java constant concatenation.
 *
 * Security: Uses correct SpEL string concatenation pattern (verified against TestParameterController).
 * Each status transition is a separate endpoint with its own @PreAuthorize.
 *
 * Base path: /api/test-executions
 * Endpoints: 13 total (4 GET, 9 POST)
 */
@RestController
@RequestMapping("/api/test-executions")
@RequiredArgsConstructor
public class TestExecutionController {

    private final TestExecutionService service;

    // --- READ endpoints ---

    @GetMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_VIEW + "')")
    public ResponseEntity<ApiResponse<List<TestExecutionSummaryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.listAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_VIEW + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @GetMapping("/ar/{arNumber}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_VIEW + "')")
    public ResponseEntity<ApiResponse<List<TestExecutionSummaryResponse>>> getByArNumber(@PathVariable String arNumber) {
        return ResponseEntity.ok(ApiResponse.ok(service.listByArNumber(arNumber)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_ALLOCATE + "')")
    public ResponseEntity<ApiResponse<List<InwardResponse>>> getPendingAllocations() {
        return ResponseEntity.ok(ApiResponse.ok(service.getPendingAllocations()));
    }

    // --- ALLOCATION ---

    @PostMapping("/allocate")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_ALLOCATE + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> allocate(
            @Valid @RequestBody TestAllocationRequest request) {
        TestExecutionResponse response = service.allocateTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // --- REASSIGNMENT ---

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_REASSIGN + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> reassign(
            @PathVariable Long id,
            @Valid @RequestBody TestReassignmentRequest request) {
        TestExecutionResponse response = service.reassignTest(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- STATE TRANSITIONS ---

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_EXECUTE + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> startTesting(
            @PathVariable Long id) {
        TestExecutionResponse response = service.startTesting(id, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_HOLD + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> putOnHold(
            @PathVariable Long id,
            @Valid @RequestBody TestStatusUpdateRequest request) {
        TestExecutionResponse response = service.putOnHold(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_CANCEL + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> cancelTest(
            @PathVariable Long id,
            @Valid @RequestBody TestStatusUpdateRequest request) {
        TestExecutionResponse response = service.cancelTest(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/submit-result")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_SUBMIT + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> submitResult(
            @PathVariable Long id,
            @Valid @RequestBody TestResultSubmissionRequest request) {
        TestExecutionResponse response = service.submitResult(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_APPROVE + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> approveRelease(
            @PathVariable Long id) {
        TestExecutionResponse response = service.approveRelease(id, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/rework")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_APPROVE + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> requestRework(
            @PathVariable Long id,
            @Valid @RequestBody TestStatusUpdateRequest request) {
        TestExecutionResponse response = service.requestRework(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_EXECUTE + "')")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> resumeFromRework(
            @PathVariable Long id) {
        TestExecutionResponse response = service.resumeFromRework(id, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
