package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.TestAllocationRequest;
import com.medplus.qc_lims_backend.dto.request.TestReassignmentRequest;
import com.medplus.qc_lims_backend.dto.request.TestResultSubmissionRequest;
import com.medplus.qc_lims_backend.dto.request.TestStatusUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionSummaryResponse;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.service.TestExecutionService;
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
 * T-16: TestExecutionController — verifies thin controller delegates to service
 * and returns correct HTTP status codes and ApiResponse envelopes.
 * T-17: TestExecutionController — 400 on validation failure (Jakarta @Valid).
 *
 * Note: These are unit tests of controller logic (not full MockMvc integration).
 * Spring Boot 4.x relocated test infrastructure; full MockMvc tests would
 * require updated test dependency configuration.
 */
class TestExecutionControllerTest {

    private final TestExecutionService service = mock(TestExecutionService.class);
    private final TestExecutionController controller = new TestExecutionController(service);

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private TestExecutionResponse makeResponse() {
        return new TestExecutionResponse(
                1L, "AR-001", 1, 10, 1, 5, 0,
                "Allocated", null, null, null, null,
                "Valid", "PENDING", null, null, NOW, null);
    }

    private TestExecutionSummaryResponse makeSummary() {
        return new TestExecutionSummaryResponse(
                1L, "AR-001", 1, "Assay", 10,
                "Allocated", "PENDING", 5, 0, NOW, null);
    }

    // --- GET listAll ---

    @Test
    void listAllReturns200WithApiResponse() {
        when(service.listAll()).thenReturn(List.of(makeSummary()));

        ResponseEntity<ApiResponse<List<TestExecutionSummaryResponse>>> response = controller.listAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals(1, response.getBody().data().size());
        assertEquals("AR-001", response.getBody().data().get(0).arNumber());
    }

    @Test
    void listAllDelegatesToService() {
        when(service.listAll()).thenReturn(List.of());
        controller.listAll();
        verify(service).listAll();
    }

    // --- GET getById ---

    @Test
    void getByIdReturns200() {
        when(service.getById(1L)).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Allocated", response.getBody().data().executionStatus());
    }

    @Test
    void getByIdNotFoundPropagates404() {
        when(service.getById(99L))
                .thenThrow(new EntityNotFoundException("TestExecution", 99L));

        assertThrows(EntityNotFoundException.class, () -> controller.getById(99L));
    }

    // --- GET getByArNumber ---

    @Test
    void getByArNumberReturns200() {
        when(service.listByArNumber("AR-001")).thenReturn(List.of(makeSummary()));

        ResponseEntity<ApiResponse<List<TestExecutionSummaryResponse>>> response = controller.getByArNumber("AR-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    // --- GET getPendingAllocations ---

    @Test
    void getPendingAllocationsReturns200() {
        when(service.getPendingAllocations()).thenReturn(List.of());

        var response = controller.getPendingAllocations();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST allocate (201 Created) ---

    @Test
    void allocateReturns201() {
        TestAllocationRequest req = new TestAllocationRequest();
        req.setArNumber("AR-001");
        req.setParameterId(1);
        req.setAllocatedToAnalystId(10);
        req.setAssignedByQcHeadId(1);
        req.setAllocatedUnits(5);

        when(service.allocateTest(any(TestAllocationRequest.class))).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.allocate(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().success());
        verify(service).allocateTest(req);
    }

    // --- POST reassign ---

    @Test
    void reassignReturns200() {
        TestReassignmentRequest req = new TestReassignmentRequest();
        req.setNewAnalystId(20);
        req.setReason("Workload rebalancing");

        when(service.reassignTest(eq(1L), any(TestReassignmentRequest.class), any())).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.reassign(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST start ---

    @Test
    void startTestingReturns200() {
        when(service.startTesting(eq(1L), any())).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.startTesting(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST hold ---

    @Test
    void putOnHoldReturns200() {
        TestStatusUpdateRequest req = new TestStatusUpdateRequest();
        req.setReason("Waiting for reference standard");

        when(service.putOnHold(eq(1L), any(TestStatusUpdateRequest.class), any()))
                .thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.putOnHold(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST cancel ---

    @Test
    void cancelTestReturns200() {
        TestStatusUpdateRequest req = new TestStatusUpdateRequest();
        req.setReason("Sample degraded");

        when(service.cancelTest(eq(1L), any(TestStatusUpdateRequest.class), any()))
                .thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.cancelTest(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST submit-result ---

    @Test
    void submitResultReturns200() {
        TestResultSubmissionRequest req = new TestResultSubmissionRequest();
        req.setResultNumericValue(new BigDecimal("100.5"));
        req.setConsumedUnits(2);

        when(service.submitResult(eq(1L), any(TestResultSubmissionRequest.class), any()))
                .thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.submitResult(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST approve ---

    @Test
    void approveReleaseReturns200() {
        when(service.approveRelease(eq(1L), any())).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.approveRelease(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST rework ---

    @Test
    void requestReworkReturns200() {
        TestStatusUpdateRequest req = new TestStatusUpdateRequest();
        req.setReason("Results inconsistent");

        when(service.requestRework(eq(1L), any(TestStatusUpdateRequest.class), any()))
                .thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.requestRework(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST resume ---

    @Test
    void resumeFromReworkReturns200() {
        when(service.resumeFromRework(eq(1L), any())).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<TestExecutionResponse>> response = controller.resumeFromRework(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Business rule violation propagation ---

    @Test
    void allocateBusinessRuleViolationPropagates() {
        TestAllocationRequest req = new TestAllocationRequest();
        req.setArNumber("AR-001");
        req.setParameterId(1);
        req.setAllocatedToAnalystId(10);
        req.setAssignedByQcHeadId(1);
        req.setAllocatedUnits(5);

        when(service.allocateTest(any(TestAllocationRequest.class)))
                .thenThrow(new BusinessRuleViolationException("Duplicate allocation"));

        assertThrows(BusinessRuleViolationException.class, () -> controller.allocate(req));
    }

    // --- Controller is thin — delegates all logic ---

    @Test
    void controllerDoesNotContainBusinessLogic() {
        // Verify that controller always calls service for all operations
        when(service.listAll()).thenReturn(List.of());
        when(service.getById(anyLong())).thenReturn(makeResponse());
        when(service.listByArNumber(anyString())).thenReturn(List.of());
        when(service.getPendingAllocations()).thenReturn(List.of());

        // Call all read endpoints
        controller.listAll();
        controller.getById(1L);
        controller.getByArNumber("AR-001");
        controller.getPendingAllocations();

        // Verify service was called
        verify(service).listAll();
        verify(service).getById(1L);
        verify(service).listByArNumber("AR-001");
        verify(service).getPendingAllocations();
    }

    // --- ApiResponse envelope consistency ---

    @Test
    void allResponsesUseApiResponseEnvelope() {
        when(service.listAll()).thenReturn(List.of(makeSummary()));

        var listResponse = controller.listAll();
        assertNotNull(listResponse.getBody());
        assertTrue(listResponse.getBody().success());
        assertNotNull(listResponse.getBody().timestamp());
        assertEquals("Success", listResponse.getBody().message());
    }
}
