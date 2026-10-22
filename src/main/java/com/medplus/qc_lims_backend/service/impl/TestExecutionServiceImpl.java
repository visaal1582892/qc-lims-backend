package com.medplus.qc_lims_backend.service.impl;

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
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionResponse;
import com.medplus.qc_lims_backend.dto.response.TestExecutionSummaryResponse;
import com.medplus.qc_lims_backend.enums.ExecutionStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.enums.ReviewStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.QcInventoryRepository;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import com.medplus.qc_lims_backend.service.InwardService;
import com.medplus.qc_lims_backend.service.QcInventoryService;
import com.medplus.qc_lims_backend.service.TestExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of TestExecutionService for Test Allocation & Execution State Machine (SRS Module 6).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestExecutionServiceImpl implements TestExecutionService {

    private final TestExecutionRepository testExecutionRepository;
    private final SampleInwardRepository inwardRepository;
    private final TestParameterMasterRepository parameterRepository;
    private final QcInventoryRepository qcInventoryRepository;
    private final QcInventoryService qcInventoryService;
    private final InwardService inwardService;
    private final ApplicationEventPublisher eventPublisher;

    // --- READ operations (readOnly by default) ---

    @Override
    public List<TestExecutionSummaryResponse> listAll() {
        List<TestExecution> executions = testExecutionRepository.findByStatus(RecordStatus.ACTIVE);
        return mapToSummaryList(executions);
    }

    @Override
    public TestExecutionResponse getById(Long id) {
        TestExecution entity = testExecutionRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestExecution", id));
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    public List<TestExecutionSummaryResponse> listByArNumber(String arNumber) {
        List<TestExecution> executions = testExecutionRepository.findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE);
        return mapToSummaryList(executions);
    }

    @Override
    public List<TestExecutionSummaryResponse> listByAnalyst(Integer analystId) {
        List<TestExecution> executions = testExecutionRepository.findByAllocatedToAnalystIdAndStatus(analystId, RecordStatus.ACTIVE);
        return mapToSummaryList(executions);
    }

    @Override
    public List<TestExecutionSummaryResponse> listByStatus(String executionStatus) {
        List<TestExecution> executions = testExecutionRepository.findByExecutionStatusAndStatus(executionStatus, RecordStatus.ACTIVE);
        return mapToSummaryList(executions);
    }

    @Override
    public List<InwardResponse> getPendingAllocations() {
        List<String> activeArNumbers = testExecutionRepository.findArNumbersWithActiveExecutions(RecordStatus.ACTIVE);
        List<SampleInwardRegister> allInward = inwardRepository.findByStatus(RecordStatus.ACTIVE);

        return allInward.stream()
                .filter(inward -> inward.getArNumber() != null && !inward.getArNumber().isBlank())
                .filter(inward -> !activeArNumbers.contains(inward.getArNumber()))
                .filter(inward -> inward.getIsSubcontracted() == null || !inward.getIsSubcontracted())
                .map(InwardResponse::fromEntity)
                .toList();
    }

    // --- ALLOCATION (mutating) ---

    @Override
    @Transactional
    public TestExecutionResponse allocateTest(TestAllocationRequest request) {
        // BR-02: NULL/blank ar_number rejection (subcontracted samples)
        if (request.getArNumber() == null || request.getArNumber().isBlank()) {
            throw new BusinessRuleViolationException("AR number is required — subcontracted samples cannot be allocated.");
        }

        // V-01: Validate inward record exists and is ACTIVE
        SampleInwardRegister inward = inwardRepository.findByArNumberAndStatus(
                request.getArNumber(), RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration with AR number", request.getArNumber()));

        // Additional check: reject subcontracted samples
        if (Boolean.TRUE.equals(inward.getIsSubcontracted())) {
            throw new BusinessRuleViolationException(
                    "Cannot allocate test for subcontracted sample: " + request.getArNumber());
        }

        // V-02: Validate parameter exists and is ACTIVE
        parameterRepository.findByIdAndStatus(request.getParameterId().longValue(), RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestParameter", request.getParameterId()));

        // BR-01: No duplicate active allocation
        List<String> terminalStatuses = List.of("Released", "Cancelled_Not_Performed");
        boolean hasActive = testExecutionRepository.existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
                request.getArNumber(), request.getParameterId(), terminalStatuses, RecordStatus.ACTIVE);
        if (hasActive) {
            throw new BusinessRuleViolationException(
                    "An active test execution already exists for AR number "
                    + request.getArNumber() + " and parameter ID " + request.getParameterId()
                    + ". Cancel the existing execution before re-allocating.");
        }

        // BR-09: Find inventory record for ar_number
        Optional<QcInventory> inventoryOpt = qcInventoryRepository.findByArNumberAndStatus(
                request.getArNumber(), RecordStatus.ACTIVE);
        if (inventoryOpt.isEmpty()) {
            throw new EntityNotFoundException("QcInventory", request.getArNumber());
        }
        QcInventory inventory = inventoryOpt.get();

        // BR-11: allocatedUnits validation
        if (request.getAllocatedUnits() < 1) {
            throw new BusinessRuleViolationException("Allocated units must be at least 1.");
        }
        int currentBalance = inventory.getCurrentBalance() != null ? inventory.getCurrentBalance()
                : inventory.getTotalQuantityReceived() - (inventory.getQuantityConsumed() != null ? inventory.getQuantityConsumed() : 0);
        if (request.getAllocatedUnits() > currentBalance) {
            throw new BusinessRuleViolationException(
                    "Cannot allocate " + request.getAllocatedUnits() + " units — "
                    + "current inventory balance is " + currentBalance + " for AR " + request.getArNumber());
        }

        // BR-08: Call QcInventoryService.allocateQuantity() — NOT direct repo update
        AllocateRequest allocReq = new AllocateRequest();
        allocReq.setQuantity(request.getAllocatedUnits());
        qcInventoryService.allocateQuantity(inventory.getId(), allocReq);

        // Create TestExecution entity
        TestExecution entity = new TestExecution();
        entity.setArNumber(request.getArNumber());
        entity.setParameterId(request.getParameterId());
        entity.setAllocatedToAnalystId(request.getAllocatedToAnalystId());
        entity.setAssignedByQcHeadId(request.getAssignedByQcHeadId());
        entity.setAllocatedUnits(request.getAllocatedUnits());
        entity.setConsumedUnits(0);
        entity.setExecutionStatus(ExecutionStatus.ALLOCATED.getDbValue());
        entity.setComplianceFlag("PENDING");
        entity.setValidityStatus("Valid");
        entity.setCreatedAt(Instant.now());
        entity.setStatus(RecordStatus.ACTIVE);

        entity = testExecutionRepository.save(entity);
        log.info("Test allocated: executionId={}, arNumber={}, parameterId={}, analystId={}",
                entity.getId(), entity.getArNumber(), entity.getParameterId(), entity.getAllocatedToAnalystId());
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    // --- REASSIGNMENT (mutating) ---

    @Override
    @Transactional
    public TestExecutionResponse reassignTest(Long executionId, TestReassignmentRequest request, Long qcHeadId) {
        TestExecution entity = loadActive(executionId);

        // Validate current status allows reassignment (Allocated or Testing)
        ExecutionStatus current = ExecutionStatus.fromDbValue(entity.getExecutionStatus());
        if (current == null || (current != ExecutionStatus.ALLOCATED && current != ExecutionStatus.TESTING)) {
            throw new BusinessRuleViolationException(
                    "Cannot reassign test in status '" + entity.getExecutionStatus()
                    + "'. Only Allocated or Testing tests can be reassigned.");
        }

        // Validate new analyst is different
        if (request.getNewAnalystId().equals(entity.getAllocatedToAnalystId())) {
            throw new BusinessRuleViolationException(
                    "Cannot reassign to the same analyst (ID: " + request.getNewAnalystId() + ").");
        }

        // Log reassignment in reworkReason field with prefix
        String reassignmentLog = String.format(
                "[Reassigned on %s: %s] (from analyst %d to %d)",
                Instant.now(), request.getReason(),
                entity.getAllocatedToAnalystId(), request.getNewAnalystId());

        String existing = entity.getReworkReason();
        entity.setReworkReason(existing != null && !existing.isBlank()
                ? existing + "\n" + reassignmentLog
                : reassignmentLog);

        entity.setAllocatedToAnalystId(request.getNewAnalystId());
        entity = testExecutionRepository.save(entity);

        log.info("Test reassigned: executionId={}, from={}, to={}",
                executionId, entity.getAllocatedToAnalystId(), request.getNewAnalystId());
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    // --- STATE TRANSITIONS (mutating) ---

    @Override
    @Transactional
    public TestExecutionResponse startTesting(Long executionId, Long analystId) {
        TestExecution entity = loadActive(executionId);
        validateTransition(executionId, entity, ExecutionStatus.TESTING);

        entity.setExecutionStatus(ExecutionStatus.TESTING.getDbValue());
        entity = testExecutionRepository.save(entity);

        log.info("Test started: executionId={}", executionId);
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse putOnHold(Long executionId, TestStatusUpdateRequest request, Long userId) {
        TestExecution entity = loadActive(executionId);

        // BR-03: Mandatory reason for Hold
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessRuleViolationException("Hold reason is mandatory.");
        }

        validateTransition(executionId, entity, ExecutionStatus.HOLD);

        entity.setExecutionStatus(ExecutionStatus.HOLD.getDbValue());
        entity.setHoldOrCancelReason(request.getReason());
        entity = testExecutionRepository.save(entity);

        log.info("Test put on hold: executionId={}, reason={}", executionId, request.getReason());
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse cancelTest(Long executionId, TestStatusUpdateRequest request, Long qcHeadId) {
        TestExecution entity = loadActive(executionId);

        // BR-04: Mandatory reason for Cancel
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessRuleViolationException("Cancel reason is mandatory.");
        }

        validateTransition(executionId, entity, ExecutionStatus.CANCELLED_NOT_PERFORMED);

        entity.setExecutionStatus(ExecutionStatus.CANCELLED_NOT_PERFORMED.getDbValue());
        entity.setHoldOrCancelReason(request.getReason());
        entity = testExecutionRepository.save(entity);

        log.info("Test cancelled: executionId={}, reason={}", executionId, request.getReason());
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse submitResult(Long executionId, TestResultSubmissionRequest request, Long analystId) {
        TestExecution entity = loadActive(executionId);

        // BR-10: At least one result value required
        if ((request.getResultNumericValue() == null)
                && (request.getResultTextValue() == null || request.getResultTextValue().isBlank())) {
            throw new BusinessRuleViolationException(
                    "At least one of resultNumericValue or resultTextValue must be provided.");
        }

        validateTransition(executionId, entity, ExecutionStatus.UNDER_REVIEW);

        // Set result values
        entity.setResultNumericValue(request.getResultNumericValue());
        entity.setResultTextValue(request.getResultTextValue());
        entity.setConsumedUnits(request.getConsumedUnits());
        entity.setSubmittedAt(Instant.now());

        // Set validity status (default "Valid")
        if (request.getValidityStatus() != null && !request.getValidityStatus().isBlank()) {
            entity.setValidityStatus(request.getValidityStatus());
        } else {
            entity.setValidityStatus("Valid");
        }

        // BR-06: Calculate compliance flag if not explicitly provided
        if (request.getComplianceFlag() != null && !request.getComplianceFlag().isBlank()) {
            entity.setComplianceFlag(request.getComplianceFlag());
        } else {
            entity.setComplianceFlag(calculateComplianceFlag(entity, request));
        }

        entity.setExecutionStatus(ExecutionStatus.UNDER_REVIEW.getDbValue());
        entity.setReviewStatus(ReviewStatus.PENDING_QC.getDbValue());
        entity = testExecutionRepository.save(entity);

        // BR-08: Record consumption via QcInventoryService
        if (request.getConsumedUnits() != null && request.getConsumedUnits() > 0) {
            Optional<QcInventory> invOpt = qcInventoryRepository.findByArNumberAndStatus(
                    entity.getArNumber(), RecordStatus.ACTIVE);
            if (invOpt.isPresent()) {
                ConsumeRequest consumeReq = new ConsumeRequest();
                consumeReq.setQuantity(request.getConsumedUnits());
                qcInventoryService.recordConsumption(invOpt.get().getId(), consumeReq);
            }
        }

        log.info("Result submitted: executionId={}, compliance={}", executionId, entity.getComplianceFlag());

        // Phase 6: OOS auto-trigger — if compliance is FAIL/OOS, publish event for OOS incident creation
        if ("Fail".equalsIgnoreCase(entity.getComplianceFlag()) || "OOS".equalsIgnoreCase(entity.getComplianceFlag())) {
            try {
                // Derive product/batch context from the linked SampleInwardRegister
                String productName = null;
                Integer productId = null;
                String batchNo = null;
                Optional<SampleInwardRegister> inwardOpt = inwardRepository
                        .findByArNumberAndStatus(entity.getArNumber(), RecordStatus.ACTIVE);
                if (inwardOpt.isPresent()) {
                    SampleInwardRegister inward = inwardOpt.get();
                    productId = inward.getProductId();
                    batchNo = inward.getBatchNo();
                }

                // Get test context from parameter master
                String testCode = null;
                String specificationReference = null;
                String unitOfMeasurement = null;
                String testMethodName = null;
                Optional<TestParameterMaster> paramOpt = parameterRepository.findById(entity.getParameterId().longValue());
                if (paramOpt.isPresent()) {
                    TestParameterMaster param = paramOpt.get();
                    testCode = param.getParameterName();
                    specificationReference = param.getMinLimit() != null && param.getMaxLimit() != null
                            ? param.getMinLimit().toPlainString() + "-" + param.getMaxLimit().toPlainString()
                            : param.getTextSpecification();
                    testMethodName = param.getStandardReference();
                }

                com.medplus.qc_lims_backend.event.TestResultOosEvent oosEvent =
                    new com.medplus.qc_lims_backend.event.TestResultOosEvent(
                        this,
                        entity.getId(),
                        entity.getArNumber(),
                        testCode,
                        productName,
                        productId,
                        batchNo,
                        specificationReference,
                        entity.getResultNumericValue() != null ? entity.getResultNumericValue().toPlainString()
                                : entity.getResultTextValue(),
                        unitOfMeasurement,
                        testMethodName,
                        analystId
                    );
                eventPublisher.publishEvent(oosEvent);
                log.info("OOS event published for executionId={}", executionId);
            } catch (Exception e) {
                log.error("Failed to publish OOS event for executionId={}: {}",
                        executionId, e.getMessage(), e);
            }
        }

        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse approveRelease(Long executionId, Long reviewerId) {
        TestExecution entity = loadActive(executionId);
        validateTransition(executionId, entity, ExecutionStatus.RELEASED);

        entity.setExecutionStatus(ExecutionStatus.RELEASED.getDbValue());
        entity = testExecutionRepository.save(entity);

        log.info("Test approved/released: executionId={}", executionId);
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse requestRework(Long executionId, TestStatusUpdateRequest request, Long reviewerId) {
        TestExecution entity = loadActive(executionId);

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessRuleViolationException("Rework reason is mandatory.");
        }

        validateTransition(executionId, entity, ExecutionStatus.REWORK_REQUESTED);

        entity.setExecutionStatus(ExecutionStatus.REWORK_REQUESTED.getDbValue());

        // Append rework reason, preserving any prior reassignment audit log
        String existing = entity.getReworkReason();
        String reworkEntry = String.format("[Rework requested on %s: %s]", Instant.now(), request.getReason());
        entity.setReworkReason(existing != null && !existing.isBlank()
                ? existing + "\n" + reworkEntry
                : reworkEntry);
        entity = testExecutionRepository.save(entity);

        log.info("Rework requested: executionId={}, reason={}", executionId, request.getReason());
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    @Override
    @Transactional
    public TestExecutionResponse resumeFromRework(Long executionId, Long analystId) {
        TestExecution entity = loadActive(executionId);
        validateTransition(executionId, entity, ExecutionStatus.TESTING);

        entity.setExecutionStatus(ExecutionStatus.TESTING.getDbValue());
        entity.setReworkReason(null);
        entity = testExecutionRepository.save(entity);

        log.info("Rework resumed: executionId={}", executionId);
        refreshArStatus(entity.getArNumber());
        return TestExecutionResponse.fromEntity(entity);
    }

    // --- Private helpers ---

    private TestExecution loadActive(Long id) {
        return testExecutionRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestExecution", id));
    }

    private void validateTransition(Long executionId, TestExecution entity, ExecutionStatus target) {
        ExecutionStatus current = ExecutionStatus.fromDbValue(entity.getExecutionStatus());
        if (current == null) {
            throw new BusinessRuleViolationException(
                    "Unknown execution status: " + entity.getExecutionStatus());
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(
                    "Invalid state transition: " + current.getDbValue() + " -> " + target.getDbValue()
                    + ". Allowed transitions from " + current.getDbValue() + ": "
                    + allowedTargets(current));
        }
    }

    private String allowedTargets(ExecutionStatus status) {
        return java.util.Arrays.stream(ExecutionStatus.values())
                .filter(target -> status.canTransitionTo(target))
                .map(ExecutionStatus::getDbValue)
                .collect(Collectors.joining(", "));
    }

    /**
     * BR-06: Compliance flag calculation.
     */
    private String calculateComplianceFlag(TestExecution entity, TestResultSubmissionRequest request) {
        Optional<TestParameterMaster> paramOpt = parameterRepository.findByIdAndStatus(
                entity.getParameterId().longValue(), RecordStatus.ACTIVE);
        if (paramOpt.isEmpty()) {
            return "PENDING";
        }

        TestParameterMaster param = paramOpt.get();

        // Numeric result comparison
        if (request.getResultNumericValue() != null) {
            BigDecimal value = request.getResultNumericValue();
            if (param.getMinLimit() != null && value.compareTo(param.getMinLimit()) < 0) {
                return "FAIL";
            }
            if (param.getMaxLimit() != null && value.compareTo(param.getMaxLimit()) > 0) {
                return "FAIL";
            }
            if (param.getMinLimit() != null || param.getMaxLimit() != null) {
                return "PASS";
            }
        }

        // Text result comparison
        if (request.getResultTextValue() != null && !request.getResultTextValue().isBlank()) {
            if (param.getTextSpecification() != null && !param.getTextSpecification().isBlank()) {
                String observed = request.getResultTextValue().toLowerCase();
                String expected = param.getTextSpecification().toLowerCase();
                if (observed.contains(expected) || expected.contains(observed)) {
                    return "PASS";
                }
            }
            return "PENDING";
        }

        return "PENDING";
    }

    /**
     * Map a list of TestExecution entities to summary responses, resolving parameter names.
     * Uses a single batch query to avoid N+1 database calls.
     */
    private List<TestExecutionSummaryResponse> mapToSummaryList(List<TestExecution> executions) {
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
                .map(entity -> TestExecutionSummaryResponse.fromEntity(
                        entity,
                        paramNameMap.getOrDefault(entity.getParameterId(), "Unknown")))
                .toList();
    }

    /**
     * Recalculate AR status and completion rate after a test status change.
     */
    private void refreshArStatus(String arNumber) {
        long total = testExecutionRepository.countByArNumberAndStatus(arNumber, RecordStatus.ACTIVE);
        long terminal = testExecutionRepository.countTerminalByArNumber(arNumber, RecordStatus.ACTIVE);
        inwardService.updateTestCompletionAndStatus(arNumber, total, terminal);
    }
}
