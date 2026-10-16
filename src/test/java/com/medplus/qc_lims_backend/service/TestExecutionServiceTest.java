package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.QcInventory;
import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.dto.request.AllocateRequest;
import com.medplus.qc_lims_backend.dto.request.ConsumeRequest;
import com.medplus.qc_lims_backend.dto.request.TestAllocationRequest;
import com.medplus.qc_lims_backend.dto.request.TestReassignmentRequest;
import com.medplus.qc_lims_backend.dto.request.TestResultSubmissionRequest;
import com.medplus.qc_lims_backend.dto.request.TestStatusUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionSummaryResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.QcInventoryRepository;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
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
 * Tests for TestExecutionService covering allocation, state transitions,
 * compliance calculation, and business rule enforcement.
 *
 * Test matrix:
 *   T-01: allocateTest success path
 *   T-02: allocateTest NULL ar_number rejected
 *   T-03: allocateTest non-existent ar_number rejected
 *   T-04: allocateTest duplicate allocation rejected
 *   T-05: allocateTest inventory integration verified
 *   T-06: reassignTest success path
 *   T-07: reassignTest same analyst rejected
 *   T-08: putOnHold success with reason
 *   T-09: putOnHold empty reason rejected
 *   T-10: cancelTest success with reason
 *   T-11: submitResult numeric PASS
 *   T-12: submitResult numeric FAIL
 *   T-13: submitResult text result
 *   T-14: submitResult no result value rejected (BR-10)
 */
@ExtendWith(MockitoExtension.class)
class TestExecutionServiceTest {

    @Mock private TestExecutionRepository testExecutionRepository;
    @Mock private SampleInwardRepository inwardRepository;
    @Mock private TestParameterMasterRepository parameterRepository;
    @Mock private QcInventoryRepository qcInventoryRepository;
    @Mock private QcInventoryService qcInventoryService;

    @InjectMocks private TestExecutionService service;

    private SampleInwardRegister sampleInward;
    private TestParameterMaster testParameter;
    private QcInventory inventory;
    private TestAllocationRequest allocationRequest;

    @BeforeEach
    void setUp() {
        // Sample inward record
        sampleInward = new SampleInwardRegister();
        sampleInward.setId(1L);
        sampleInward.setArNumber("AR-001");
        sampleInward.setProductId(100);
        sampleInward.setBatchNo("BATCH-001");
        sampleInward.setReceivedQuantityPacks(10);
        sampleInward.setTotalReceivedUnits(100);
        sampleInward.setIsSubcontracted(false);
        sampleInward.setStatus(RecordStatus.ACTIVE);

        // Test parameter with limits
        testParameter = new TestParameterMaster();
        testParameter.setId(1L);
        testParameter.setParameterName("Assay");
        testParameter.setMinLimit(new BigDecimal("95.0"));
        testParameter.setMaxLimit(new BigDecimal("105.0"));
        testParameter.setStatus(RecordStatus.ACTIVE);

        // Inventory record
        inventory = new QcInventory();
        inventory.setId(1L);
        inventory.setArNumber("AR-001");
        inventory.setProductId(100);
        inventory.setBatchNo("BATCH-001");
        inventory.setTotalQuantityReceived(100);
        inventory.setQuantityAllocated(0);
        inventory.setQuantityConsumed(0);
        inventory.setStatus(RecordStatus.ACTIVE);

        // Allocation request
        allocationRequest = new TestAllocationRequest();
        allocationRequest.setArNumber("AR-001");
        allocationRequest.setParameterId(1);
        allocationRequest.setAllocatedToAnalystId(10);
        allocationRequest.setAssignedByQcHeadId(1);
        allocationRequest.setAllocatedUnits(5);
    }

    // Helper: build a saved TestExecution entity
    private TestExecution buildExecution(String status) {
        TestExecution e = new TestExecution();
        e.setId(1L);
        e.setArNumber("AR-001");
        e.setParameterId(1);
        e.setAllocatedToAnalystId(10);
        e.setAssignedByQcHeadId(1);
        e.setAllocatedUnits(5);
        e.setConsumedUnits(0);
        e.setExecutionStatus(status);
        e.setComplianceFlag("PENDING");
        e.setValidityStatus("Valid");
        e.setCreatedAt(Instant.now());
        e.setStatus(RecordStatus.ACTIVE);
        return e;
    }

    // --- T-01: allocateTest success path ---

    @Test
    void t01AllocateTestSuccess() {
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
                eq("AR-001"), eq(1), anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(false);
        when(qcInventoryRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(inventory));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestExecutionResponse response = service.allocateTest(allocationRequest);

        assertNotNull(response);
        assertEquals("Allocated", response.executionStatus());
        assertEquals("PENDING", response.complianceFlag());
        assertEquals("Valid", response.validityStatus());
        assertEquals(0, response.consumedUnits());
        assertEquals(5, response.allocatedUnits());

        // Verify inventory allocation was called
        ArgumentCaptor<AllocateRequest> allocCaptor = ArgumentCaptor.forClass(AllocateRequest.class);
        verify(qcInventoryService).allocateQuantity(eq(1L), allocCaptor.capture());
        assertEquals(5, allocCaptor.getValue().getQuantity());

        // Verify entity was saved
        verify(testExecutionRepository).save(any(TestExecution.class));
    }

    // --- T-02: allocateTest NULL ar_number rejected ---

    @Test
    void t02AllocateTestNullArNumberRejected() {
        allocationRequest.setArNumber(null);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.allocateTest(allocationRequest));

        verifyNoInteractions(testExecutionRepository);
    }

    @Test
    void t02bAllocateTestBlankArNumberRejected() {
        allocationRequest.setArNumber("  ");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.allocateTest(allocationRequest));
    }

    // --- T-03: allocateTest non-existent ar_number rejected ---

    @Test
    void t03AllocateTestNonExistentArNumberRejected() {
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.allocateTest(allocationRequest));
    }

    // --- T-04: allocateTest duplicate allocation rejected ---

    @Test
    void t04AllocateTestDuplicateAllocationRejected() {
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
                eq("AR-001"), eq(1), anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(true);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.allocateTest(allocationRequest));
    }

    // --- T-05: allocateTest inventory integration verified ---

    @Test
    void t05AllocateTestInventoryIntegration() {
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
                eq("AR-001"), eq(1), anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(false);
        when(qcInventoryRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(inventory));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.allocateTest(allocationRequest);

        verify(qcInventoryService, times(1)).allocateQuantity(eq(1L), any(AllocateRequest.class));
        verify(testExecutionRepository, times(1)).save(any(TestExecution.class));
    }

    // --- T-06: reassignTest success path ---

    @Test
    void t06ReassignTestSuccess() {
        TestExecution entity = buildExecution("Allocated");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestReassignmentRequest request = new TestReassignmentRequest();
        request.setNewAnalystId(20);
        request.setReason("Workload rebalancing");

        TestExecutionResponse response = service.reassignTest(1L, request, 1L);

        assertEquals(20, response.allocatedToAnalystId());
        assertNotNull(response.reworkReason());
        assertTrue(response.reworkReason().contains("Reassigned on"));
        assertTrue(response.reworkReason().contains("Workload rebalancing"));
    }

    // --- T-07: reassignTest same analyst rejected ---

    @Test
    void t07ReassignTestSameAnalystRejected() {
        TestExecution entity = buildExecution("Allocated");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestReassignmentRequest request = new TestReassignmentRequest();
        request.setNewAnalystId(10); // same as current
        request.setReason("No change");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.reassignTest(1L, request, 1L));
    }

    // --- T-08: putOnHold success with reason ---

    @Test
    void t08PutOnHoldSuccess() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        request.setReason("Waiting for reference standard");

        TestExecutionResponse response = service.putOnHold(1L, request, 1L);

        assertEquals("Hold", response.executionStatus());
        assertEquals("Waiting for reference standard", response.holdOrCancelReason());
    }

    // --- T-09: putOnHold empty reason rejected ---

    @Test
    void t09PutOnHoldEmptyReasonRejected() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        request.setReason("");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.putOnHold(1L, request, 1L));
    }

    @Test
    void t09bPutOnHoldNullReasonRejected() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        // reason is null by default

        assertThrows(BusinessRuleViolationException.class,
                () -> service.putOnHold(1L, request, 1L));
    }

    // --- T-10: cancelTest success with reason ---

    @Test
    void t10CancelTestSuccess() {
        TestExecution entity = buildExecution("Allocated");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        request.setReason("Sample degraded during storage");

        TestExecutionResponse response = service.cancelTest(1L, request, 1L);

        assertEquals("Cancelled_Not_Performed", response.executionStatus());
        assertEquals("Sample degraded during storage", response.holdOrCancelReason());
    }

    // --- T-11: submitResult numeric PASS ---

    @Test
    void t11SubmitResultNumericPass() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultNumericValue(new BigDecimal("100.5")); // Within 95-105 range
        request.setConsumedUnits(0);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);

        assertEquals("Under_Review", response.executionStatus());
        assertEquals("PASS", response.complianceFlag());
        assertNotNull(response.submittedAt());
    }

    // --- T-12: submitResult numeric FAIL ---

    @Test
    void t12SubmitResultNumericFail() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultNumericValue(new BigDecimal("80.0")); // Below 95 min
        request.setConsumedUnits(1);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);

        assertEquals("Under_Review", response.executionStatus());
        assertEquals("FAIL", response.complianceFlag());
    }

    // --- T-13: submitResult text result ---

    @Test
    void t13SubmitResultTextResult() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Text-based parameter
        TestParameterMaster textParam = new TestParameterMaster();
        textParam.setId(1L);
        textParam.setParameterName("Appearance");
        textParam.setTextSpecification("White");
        textParam.setStatus(RecordStatus.ACTIVE);
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(textParam));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultTextValue("White crystalline powder"); // Contains "white"
        request.setConsumedUnits(0);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);

        assertEquals("Under_Review", response.executionStatus());
        assertEquals("PASS", response.complianceFlag());
    }

    // --- T-14: submitResult no result value rejected (BR-10) ---

    @Test
    void t14SubmitResultNoValueRejected() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        // Neither numeric nor text value set
        request.setConsumedUnits(0);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.submitResult(1L, request, 1L));
    }

    // --- Additional: state transition validation ---

    @Test
    void cannotSubmitFromHold() {
        // Hold -> Under_Review is not a valid transition
        TestExecution entity = buildExecution("Hold");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultNumericValue(new BigDecimal("100.0"));
        request.setConsumedUnits(0);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.submitResult(1L, request, 1L));
    }

    @Test
    void cannotSubmitFromAllocated() {
        TestExecution entity = buildExecution("Allocated");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultNumericValue(new BigDecimal("100.0"));
        request.setConsumedUnits(0);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.submitResult(1L, request, 1L));
    }

    @Test
    void cannotApproveFromTesting() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        assertThrows(BusinessRuleViolationException.class,
                () -> service.approveRelease(1L, 1L));
    }

    // --- Additional: reassign from invalid status ---

    @Test
    void cannotReassignFromUnderReview() {
        TestExecution entity = buildExecution("Under_Review");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestReassignmentRequest request = new TestReassignmentRequest();
        request.setNewAnalystId(20);
        request.setReason("Reassignment");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.reassignTest(1L, request, 1L));
    }

    // --- Additional: subcontracted sample rejection ---

    @Test
    void allocateSubcontractedSampleRejected() {
        sampleInward.setIsSubcontracted(true);
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));

        assertThrows(BusinessRuleViolationException.class,
                () -> service.allocateTest(allocationRequest));
    }

    // --- Additional: inventory not found ---

    @Test
    void allocateWithNoInventoryRecordRejected() {
        when(inwardRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(sampleInward));
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(testParameter));
        when(testExecutionRepository.existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
                eq("AR-001"), eq(1), anyList(), eq(RecordStatus.ACTIVE)))
                .thenReturn(false);
        when(qcInventoryRepository.findByArNumberAndStatus("AR-001", RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.allocateTest(allocationRequest));
    }

    // --- Read operations ---

    @Test
    void listAllReturnsActiveExecutions() {
        when(testExecutionRepository.findByStatus(RecordStatus.ACTIVE)).thenReturn(List.of());

        List<TestExecutionSummaryResponse> result = service.listAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdNotFoundThrows() {
        when(testExecutionRepository.findByIdAndStatus(99L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void getPendingAllocationsReturnsInwardsWithNoActiveExecutions() {
        // No active executions exist
        when(testExecutionRepository.findArNumbersWithActiveExecutions(RecordStatus.ACTIVE))
                .thenReturn(List.of());
        when(inwardRepository.findByStatus(RecordStatus.ACTIVE))
                .thenReturn(List.of(sampleInward));

        var result = service.getPendingAllocations();
        assertEquals(1, result.size());
        assertEquals("AR-001", result.get(0).arNumber());
    }

    @Test
    void getPendingAllocationsExcludesSubcontracted() {
        when(testExecutionRepository.findArNumbersWithActiveExecutions(RecordStatus.ACTIVE))
                .thenReturn(List.of());

        SampleInwardRegister sub = new SampleInwardRegister();
        sub.setArNumber("AR-SUB-001");
        sub.setIsSubcontracted(true);
        sub.setStatus(RecordStatus.ACTIVE);

        when(inwardRepository.findByStatus(RecordStatus.ACTIVE))
                .thenReturn(List.of(sub));

        var result = service.getPendingAllocations();
        assertTrue(result.isEmpty());
    }

    // --- Rework and resume ---

    @Test
    void approveReleaseSuccess() {
        TestExecution entity = buildExecution("Under_Review");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestExecutionResponse response = service.approveRelease(1L, 1L);
        assertEquals("Released", response.executionStatus());
    }

    @Test
    void requestReworkSuccess() {
        TestExecution entity = buildExecution("Under_Review");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        request.setReason("Results need verification");

        TestExecutionResponse response = service.requestRework(1L, request, 1L);
        assertEquals("Rework_Requested", response.executionStatus());
        assertEquals("Results need verification", response.reworkReason());
    }

    @Test
    void resumeFromReworkSuccess() {
        TestExecution entity = buildExecution("Rework_Requested");
        entity.setReworkReason("Previous results invalid");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestExecutionResponse response = service.resumeFromRework(1L, 1L);
        assertEquals("Testing", response.executionStatus());
        assertNull(response.reworkReason()); // Cleared after resume
    }

    @Test
    void cancelReasonMandatory() {
        TestExecution entity = buildExecution("Allocated");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        TestStatusUpdateRequest request = new TestStatusUpdateRequest();
        request.setReason(null);

        assertThrows(BusinessRuleViolationException.class,
                () -> service.cancelTest(1L, request, 1L));
    }

    // --- Compliance: text result no match -> PENDING ---

    @Test
    void submitResultTextNoMatchReturnsPending() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestParameterMaster textParam = new TestParameterMaster();
        textParam.setId(1L);
        textParam.setTextSpecification("White");
        textParam.setStatus(RecordStatus.ACTIVE);
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(textParam));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultTextValue("Yellow powder"); // No match for "white"
        request.setConsumedUnits(0);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);
        assertEquals("PENDING", response.complianceFlag());
    }

    // --- Compliance: text result no spec -> PENDING ---

    @Test
    void submitResultTextNoSpecReturnsPending() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestParameterMaster textParam = new TestParameterMaster();
        textParam.setId(1L);
        textParam.setTextSpecification(null); // No specification
        textParam.setStatus(RecordStatus.ACTIVE);
        when(parameterRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(textParam));

        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultTextValue("Observed value");
        request.setConsumedUnits(0);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);
        assertEquals("PENDING", response.complianceFlag());
    }

    // --- Compliance: explicit override ---

    @Test
    void submitResultExplicitComplianceFlagPreserved() {
        TestExecution entity = buildExecution("Testing");
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When compliance flag is explicitly provided, service skips calculation
        // so parameterRepository is not called
        TestResultSubmissionRequest request = new TestResultSubmissionRequest();
        request.setResultNumericValue(new BigDecimal("80.0")); // Would be FAIL if calculated
        request.setComplianceFlag("PENDING"); // Override
        request.setConsumedUnits(0);

        TestExecutionResponse response = service.submitResult(1L, request, 1L);
        assertEquals("PENDING", response.complianceFlag());
    }
}
