package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.AllocateRequest;
import com.medplus.qc_lims_backend.dto.request.ConsumeRequest;
import com.medplus.qc_lims_backend.dto.request.TestAllocationRequest;
import com.medplus.qc_lims_backend.dto.request.TestReassignmentRequest;
import com.medplus.qc_lims_backend.dto.request.TestResultSubmissionRequest;
import com.medplus.qc_lims_backend.dto.request.TestStatusUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionSummaryResponse;

import java.util.List;

/**
 * Service interface for Test Allocation & Execution State Machine (SRS Module 6).
 */
public interface TestExecutionService {

    // --- READ operations ---

    List<TestExecutionSummaryResponse> listAll();

    TestExecutionResponse getById(Long id);

    List<TestExecutionSummaryResponse> listByArNumber(String arNumber);

    List<TestExecutionSummaryResponse> listByAnalyst(Integer analystId);

    List<TestExecutionSummaryResponse> listByStatus(String executionStatus);

    List<InwardResponse> getPendingAllocations();

    // --- ALLOCATION (mutating) ---

    TestExecutionResponse allocateTest(TestAllocationRequest request);

    // --- REASSIGNMENT (mutating) ---

    TestExecutionResponse reassignTest(Long executionId, TestReassignmentRequest request, Long qcHeadId);

    // --- STATE TRANSITIONS (mutating) ---

    TestExecutionResponse startTesting(Long executionId, Long analystId);

    TestExecutionResponse putOnHold(Long executionId, TestStatusUpdateRequest request, Long userId);

    TestExecutionResponse cancelTest(Long executionId, TestStatusUpdateRequest request, Long qcHeadId);

    TestExecutionResponse submitResult(Long executionId, TestResultSubmissionRequest request, Long analystId);

    TestExecutionResponse approveRelease(Long executionId, Long reviewerId);

    TestExecutionResponse requestRework(Long executionId, TestStatusUpdateRequest request, Long reviewerId);

    TestExecutionResponse resumeFromRework(Long executionId, Long analystId);
}
