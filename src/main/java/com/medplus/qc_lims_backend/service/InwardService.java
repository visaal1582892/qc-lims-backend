package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.ArLifecycleRequest;
import com.medplus.qc_lims_backend.dto.request.CreateInwardRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateInwardRequest;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.enums.ArStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service layer for Inward Registration CRUD operations.
 *
 * AR number generation via ArSequenceGenerator (atomic zero-gap, RC-4).
 * Soft delete via status=INACTIVE (RC-7).
 * Follows TestParameterService pattern exactly.
 *
 * H-4 note: Delegation to QcInventoryService on sample acceptance is deferred.
 * Per SRS Module 5.1, inventory creation should happen upon inward acceptance.
 * This will be implemented as a separate integration task once Phase 3 corrections
 * are accepted. The QcInventoryService.createInventory() method is ready to be called
 * from InwardService.create() after entity persistence succeeds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InwardService {

    private final SampleInwardRepository repository;
    private final ArSequenceGenerator arSequenceGenerator;

    // --- READ operations (readOnly by default) ---

    public List<InwardResponse> listAll() {
        return repository.findByStatus(RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public InwardResponse getById(Long id) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));
        return toResponse(entity);
    }

    public InwardResponse getByArNumber(String arNumber) {
        SampleInwardRegister entity = repository.findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration with AR number", arNumber));
        return toResponse(entity);
    }

    public List<InwardResponse> listByProductId(Integer productId) {
        return repository.findByProductIdAndStatus(productId, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InwardResponse> searchByBatchNo(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) {
            return listAll();
        }
        return repository.findByBatchNoContainingIgnoreCaseAndStatus(batchNo, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InwardResponse> listByArStatus(String arStatus) {
        return repository.findByArStatusAndStatus(arStatus, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    // --- CREATE (mutating) ---

    @Transactional
    public InwardResponse create(CreateInwardRequest request, Long createdBy) {
        // Business rule: V-08 totalReceivedUnits >= receivedQuantityPacks
        if (request.getTotalReceivedUnits() < request.getReceivedQuantityPacks()) {
            throw new BusinessRuleViolationException(
                "Total received units (" + request.getTotalReceivedUnits() +
                ") cannot be less than received quantity packs (" + request.getReceivedQuantityPacks() + ").");
        }

        // H-4: Validate subcontractRemarks is present when subcontracted
        boolean isSubcontracted = Boolean.TRUE.equals(request.getIsSubcontracted());
        if (isSubcontracted && (request.getSubcontractRemarks() == null || request.getSubcontractRemarks().isBlank())) {
            throw new BusinessRuleViolationException(
                "Subcontract remarks are required when subcontracted = true.");
        }

        SampleInwardRegister entity = new SampleInwardRegister();
        populateEntity(entity, request);
        entity.setCreatedBy(createdBy);
        entity.setStatus(RecordStatus.ACTIVE);
        entity.setReceiptDate(LocalDate.now());

        // V-01/V-02: Generate AR number for non-subcontracted; NULL for subcontracted
        if (!isSubcontracted) {
            entity.setArNumber(arSequenceGenerator.generateArNumber());
        } else {
            entity.setArNumber(null);
        }

        entity = repository.save(entity);
        return toResponse(entity);
    }

    // --- UPDATE (mutating) ---

    @Transactional
    public InwardResponse update(Long id, UpdateInwardRequest request, Long updatedBy) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        // Business rule: no AR number change after creation
        // (AR number is immutable once assigned)

        // Validate total >= packs if both provided
        if (request.getTotalReceivedUnits() != null && request.getReceivedQuantityPacks() != null) {
            if (request.getTotalReceivedUnits() < request.getReceivedQuantityPacks()) {
                throw new BusinessRuleViolationException(
                    "Total received units cannot be less than received quantity packs.");
            }
        }

        // Apply partial update — only non-null fields
        if (request.getProductId() != null) entity.setProductId(request.getProductId());
        if (request.getBatchNo() != null) entity.setBatchNo(request.getBatchNo());
        if (request.getReceivedQuantityPacks() != null) entity.setReceivedQuantityPacks(request.getReceivedQuantityPacks());
        if (request.getTotalReceivedUnits() != null) entity.setTotalReceivedUnits(request.getTotalReceivedUnits());
        if (request.getTrfId() != null) entity.setTrfId(request.getTrfId());
        if (request.getStorageLocation() != null) entity.setStorageLocation(request.getStorageLocation());
        if (request.getIsSubcontracted() != null) entity.setIsSubcontracted(request.getIsSubcontracted());
        if (request.getSubcontractRemarks() != null) entity.setSubcontractRemarks(request.getSubcontractRemarks());

        entity.setUpdatedBy(updatedBy);

        entity = repository.save(entity);
        return toResponse(entity);
    }

    // --- DELETE (mutating) — Soft delete per RC-7 ---

    @Transactional
    public void delete(Long id) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        entity.setStatus(RecordStatus.INACTIVE);
        repository.save(entity);
    }

    // --- AR LIFECYCLE — close / cancel (Phase 5) ---

    /**
     * Close an AR — allowed only when all allocated tests are terminal.
     * Transition: ALL_TESTS_COMPLETE -> COMPLETED.
     */
    @Transactional
    public InwardResponse closeAr(Long id, ArLifecycleRequest request, Long qcHeadId) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        ArStatus current = entity.getArStatusEnum();
        if (current == null || current != ArStatus.ALL_TESTS_COMPLETE) {
            throw new BusinessRuleViolationException(
                    "Cannot close AR: current status is '" + entity.getArStatus()
                    + "'. AR must be in ALL_TESTS_COMPLETE status to close.");
        }

        entity.setArStatusEnum(ArStatus.COMPLETED);
        entity.setUpdatedBy(qcHeadId);
        entity = repository.save(entity);

        log.info("AR closed: id={}, arNumber={}, by={}, reason={}",
                id, entity.getArNumber(), qcHeadId, request.getReason());
        return toResponse(entity);
    }

    /**
     * Cancel an AR — allowed from REGISTERED or IN_PROGRESS.
     * Transition: REGISTERED/IN_PROGRESS -> CANCELLED.
     */
    @Transactional
    public InwardResponse cancelAr(Long id, ArLifecycleRequest request, Long qcHeadId) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        ArStatus current = entity.getArStatusEnum();
        if (current == null || (current != ArStatus.REGISTERED && current != ArStatus.IN_PROGRESS)) {
            throw new BusinessRuleViolationException(
                    "Cannot cancel AR: current status is '" + entity.getArStatus()
                    + "'. AR must be in REGISTERED or IN_PROGRESS status to cancel.");
        }

        entity.setArStatusEnum(ArStatus.CANCELLED);
        entity.setUpdatedBy(qcHeadId);
        entity = repository.save(entity);

        log.info("AR cancelled: id={}, arNumber={}, by={}, reason={}",
                id, entity.getArNumber(), qcHeadId, request.getReason());
        return toResponse(entity);
    }

    /**
     * Recalculate and persist the test completion rate for an AR.
     * Called by TestExecutionService after each test status change.
     *
     * Uses pessimistic locking (SELECT ... FOR UPDATE) to prevent race conditions
     * when multiple test results are submitted concurrently for the same AR.
     *
     * Sets ar_status to:
     *   IN_PROGRESS — if at least one test is non-terminal
     *   ALL_TESTS_COMPLETE — if all tests are terminal
     *   REGISTERED — if no tests allocated yet
     */
    @Transactional
    public void updateTestCompletionAndStatus(String arNumber, long totalTests, long terminalTests) {
        repository.findByArNumberAndStatusForUpdate(arNumber, RecordStatus.ACTIVE).ifPresent(entity -> {
            BigDecimal rate = BigDecimal.ZERO;
            if (totalTests > 0) {
                rate = terminalTests > totalTests
                        ? new BigDecimal("100.00")
                        : new BigDecimal(terminalTests)
                                .multiply(new BigDecimal("100"))
                                .divide(new BigDecimal(totalTests), 2, java.math.RoundingMode.HALF_UP);
            }
            entity.setTestCompletionRate(rate);

            // Determine ar_status
            ArStatus newStatus;
            if (totalTests == 0) {
                newStatus = ArStatus.REGISTERED;
            } else if (terminalTests >= totalTests) {
                newStatus = ArStatus.ALL_TESTS_COMPLETE;
            } else {
                newStatus = ArStatus.IN_PROGRESS;
            }
            entity.setArStatusEnum(newStatus);

            repository.save(entity);
            log.debug("AR status updated: arNumber={}, totalTests={}, terminalTests={}, rate={}, status={}",
                    arNumber, totalTests, terminalTests, rate, newStatus);
        });
    }

    // --- Entity population helper ---

    private void populateEntity(SampleInwardRegister entity, CreateInwardRequest request) {
        entity.setProductId(request.getProductId());
        entity.setBatchNo(request.getBatchNo());
        entity.setReceivedQuantityPacks(request.getReceivedQuantityPacks());
        entity.setTotalReceivedUnits(request.getTotalReceivedUnits());
        entity.setTrfId(request.getTrfId());
        entity.setStorageLocation(request.getStorageLocation());
        entity.setIsSubcontracted(request.getIsSubcontracted() != null ? request.getIsSubcontracted() : false);
        entity.setSubcontractRemarks(request.getSubcontractRemarks());
    }

    // --- DTO conversion ---

    private InwardResponse toResponse(SampleInwardRegister entity) {
        return InwardResponse.fromEntity(entity);
    }
}
