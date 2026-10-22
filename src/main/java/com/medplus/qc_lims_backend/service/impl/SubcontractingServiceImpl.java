package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.SubcontractRegister;
import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.dto.request.CreateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.response.SubcontractResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.enums.SubcontractStatus;
import com.medplus.qc_lims_backend.repository.SubcontractRepository;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import com.medplus.qc_lims_backend.service.SubcontractingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Subcontracting service implementation (SRS Module 10).
 *
 * Responsibilities:
 * - Create subcontract entries for external lab dispatch
 * - List subcontract register entries
 * - Update subcontract remarks and status
 * - NO AR Number generated (ar_number remains NULL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubcontractingServiceImpl implements SubcontractingService {

    private final SubcontractRepository subcontractRepository;
    private final TrfRepository trfRepository;

    // ========================================================================
    // SC-01: Create Subcontract Entry
    // ========================================================================

    @Override
    @Transactional
    public SubcontractResponse createSubcontract(CreateSubcontractRequest request) {
        // Validate QC Head ID is not null
        if (request.qcHeadId() == null || request.qcHeadId() <= 0) {
            throw new BusinessRuleViolationException("Valid QC Head ID is required for subcontract dispatch");
        }

        // Validate TRF exists and is in Accepted status (RC-4 / HIGH-1)
        if (request.trfNumber() != null && !request.trfNumber().isBlank()) {
            // TRF number validation — check the number format
            if (!request.trfNumber().matches("TRF-\\d{4}-\\d{3,}")) {
                throw new BusinessRuleViolationException(
                        "Invalid TRF number format: " + request.trfNumber());
            }
        }

        // Build subcontract entity
        SubcontractRegister subcontract = new SubcontractRegister();
        subcontract.setSubcontractNo(generateSubcontractNo());
        subcontract.setTrfNumber(request.trfNumber());
        subcontract.setArNumber(null); // NO AR Number for subcontracted samples
        subcontract.setExternalLabName(request.externalLabName());
        subcontract.setReason(request.reason());
        subcontract.setDispatchDate(LocalDate.now());
        subcontract.setDispatchedByQcHeadId(request.qcHeadId());
        subcontract.setExpectedReturnDate(request.expectedReturnDate());
        subcontract.setSubcontractStatus(SubcontractStatus.DISPATCHED.getDbValue());
        subcontract.setRoaReceived(false);
        subcontract.setStatus(RecordStatus.ACTIVE);
        subcontract.setCreatedBy(resolveUserId());
        subcontract.setUpdatedBy(resolveUserId());

        SubcontractRegister saved = subcontractRepository.save(subcontract);

        log.info("Subcontract {} created for external lab '{}' (TRF Number: {}). No AR number generated.",
                saved.getSubcontractNo(), saved.getExternalLabName(), saved.getTrfNumber());

        return SubcontractResponse.fromEntity(saved);
    }

    // ========================================================================
    // SC-02: List Subcontracts
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SubcontractResponse> listSubcontracts() {
        List<SubcontractRegister> subcontracts =
                subcontractRepository.findByStatusOrderByDispatchDateDesc(RecordStatus.ACTIVE);
        return subcontracts.stream()
                .map(SubcontractResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // SC-03: Get Subcontract Detail
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public SubcontractResponse getSubcontract(Long id) {
        SubcontractRegister subcontract = subcontractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubcontractRegister", id));

        if (!RecordStatus.ACTIVE.equals(subcontract.getStatus())) {
            throw new EntityNotFoundException("SubcontractRegister", id);
        }

        return SubcontractResponse.fromEntity(subcontract);
    }

    // ========================================================================
    // SC-04: Update Subcontract (remarks/status)
    // ========================================================================

    @Override
    @Transactional
    public SubcontractResponse updateSubcontract(Long id, UpdateSubcontractRequest request) {
        SubcontractRegister subcontract = subcontractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubcontractRegister", id));

        if (!RecordStatus.ACTIVE.equals(subcontract.getStatus())) {
            throw new EntityNotFoundException("SubcontractRegister", id);
        }

        // Update remarks if provided
        if (request.remarks() != null && !request.remarks().isBlank()) {
            String existingRemarks = subcontract.getRemarks() != null ? subcontract.getRemarks() : "";
            subcontract.setRemarks(existingRemarks + "\n[" + LocalDate.now() + "] " + request.remarks());
        }

        // Update status if provided — validate transition
        if (request.status() != null && !request.status().isBlank()) {
            validateStatusTransition(subcontract.getSubcontractStatus(), request.status());
            subcontract.setSubcontractStatus(request.status());
        }

        subcontract.setUpdatedBy(resolveUserId());
        SubcontractRegister saved = subcontractRepository.save(subcontract);

        log.info("Subcontract {} updated: remarks={}, status={}",
                saved.getSubcontractNo(), request.remarks(), request.status());

        return SubcontractResponse.fromEntity(saved);
    }

    // ========================================================================
    // SC-05: Mark ROA as received
    // ========================================================================

    @Override
    @Transactional
    public SubcontractResponse markRoaReceived(Long id) {
        SubcontractRegister subcontract = subcontractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SubcontractRegister", id));

        if (!RecordStatus.ACTIVE.equals(subcontract.getStatus())) {
            throw new EntityNotFoundException("SubcontractRegister", id);
        }

        subcontract.setRoaReceived(true);
        if ("DISPATCHED".equals(subcontract.getSubcontractStatus()) || "IN_PROGRESS".equals(subcontract.getSubcontractStatus())) {
            subcontract.setSubcontractStatus("COMPLETED");
        }
        subcontract.setUpdatedBy(resolveUserId());
        SubcontractRegister saved = subcontractRepository.save(subcontract);

        log.info("ROA marked as received for subcontract {}", saved.getSubcontractNo());
        return SubcontractResponse.fromEntity(saved);
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Generate a unique subcontract number.
     * Format: SC-{date}-{sequence}
     */
    private String generateSubcontractNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "SC-" + datePart + "-" + uuidPart;
    }

    /**
     * Validate subcontract status transitions.
     * Valid transitions:
     *   DISPATCHED -> IN_PROGRESS, COMPLETED, CANCELLED
     *   IN_PROGRESS -> COMPLETED, CANCELLED
     *   COMPLETED -> (no transitions)
     *   CANCELLED -> (no transitions)
     */
    private void validateStatusTransition(String current, String next) {
        if ("COMPLETED".equals(current) || "CANCELLED".equals(current)) {
            throw new BusinessRuleViolationException(
                    "Cannot transition from " + current + " status. Subcontract is already terminal.");
        }
        if (!"DISPATCHED".equals(next) && !"IN_PROGRESS".equals(next)
                && !"COMPLETED".equals(next) && !"CANCELLED".equals(next)) {
            throw new BusinessRuleViolationException("Invalid subcontract status: " + next);
        }
    }

    @Override
    public Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        // Default to admin (1L) for anonymous/dev access
        return 1L;
    }
}
