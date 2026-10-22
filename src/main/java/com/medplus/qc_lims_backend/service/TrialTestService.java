package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.ReviewDecisionRequest;
import com.medplus.qc_lims_backend.enums.ArStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trial Test and For Information service (SRS Module 9).
 *
 * Responsibilities:
 * - Mark an AR as FOR_INFORMATION (trial test)
 * - Generate a fresh AR from a For_Information AR
 * - Link the fresh AR back to the For_Information AR via linkedForInfoArNumber
 * - Close the For_Information AR after fresh AR is generated
 *
 * Uses ArStatus.FOR_INFORMATION and ArStatus.REJECTED enum constants.
 * Email notifications are stubbed with logging only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialTestService {

    private final SampleInwardRepository sampleInwardRepository;
    private final ArSequenceGenerator arSequenceGenerator;

    // ========================================================================
    // TT-01: Mark AR as For_Information (Trial Test)
    // ========================================================================

    @Transactional
    public SampleInwardRegister markForInformation(String arNumber, String reason) {
        SampleInwardRegister inward = sampleInwardRepository
                .findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("SampleInwardRegister", arNumber));

        // Only allow FOR_INFORMATION on ARs that are in COMPLETED or ALL_TESTS_COMPLETE state
        ArStatus currentStatus = inward.getArStatusEnum();
        if (currentStatus != ArStatus.COMPLETED && currentStatus != ArStatus.ALL_TESTS_COMPLETE) {
            throw new BusinessRuleViolationException(
                    "Cannot mark AR as For_Information: current status is " + currentStatus.getDbValue() +
                    ". AR must be in COMPLETED or ALL_TESTS_COMPLETE status.");
        }

        inward.setArStatusEnum(ArStatus.FOR_INFORMATION);
        inward.setUpdatedBy(resolveUserId());
        SampleInwardRegister saved = sampleInwardRepository.save(inward);

        log.info("AR {} marked as FOR_INFORMATION. Reason: {}", saved.getArNumber(), reason);
        log.info("[EMAIL-STUB] For_Information notification would be sent for AR {}", saved.getArNumber());

        return saved;
    }

    // ========================================================================
    // TT-02: Generate Fresh AR from For_Information AR
    // ========================================================================

    @Transactional
    public SampleInwardRegister generateFreshAr(String forInfoArNumber) {
        // Source AR must be in FOR_INFORMATION status
        SampleInwardRegister source = sampleInwardRepository
                .findByArNumberAndStatus(forInfoArNumber, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("SampleInwardRegister", forInfoArNumber));

        if (source.getArStatusEnum() != ArStatus.FOR_INFORMATION) {
            throw new BusinessRuleViolationException(
                    "Cannot generate fresh AR: source AR is in " + source.getArStatusEnum().getDbValue() +
                    " status, not FOR_INFORMATION.");
        }

        // Generate new AR number
        String freshArNumber = arSequenceGenerator.generateArNumber();

        // Create the fresh AR as a copy of the source, linked back to it
        SampleInwardRegister fresh = new SampleInwardRegister();
        fresh.setArNumber(freshArNumber);
        fresh.setTrfId(source.getTrfId());
        fresh.setProductId(source.getProductId());
        fresh.setBatchNo(source.getBatchNo());
        fresh.setReceivedQuantityPacks(source.getReceivedQuantityPacks());
        fresh.setTotalReceivedUnits(source.getTotalReceivedUnits());
        fresh.setStorageLocation(source.getStorageLocation());
        fresh.setArStatusEnum(ArStatus.REGISTERED);
        fresh.setTestCompletionRate(BigDecimal.ZERO);
        fresh.setIsSubcontracted(source.getIsSubcontracted());
        fresh.setSubcontractRemarks(source.getSubcontractRemarks());
        fresh.setReceiptDate(source.getReceiptDate());

        // Link the fresh AR back to the For_Information AR
        fresh.setLinkedForInfoArNumber(forInfoArNumber);

        fresh.setStatus(RecordStatus.ACTIVE);
        fresh.setCreatedBy(resolveUserId());
        fresh.setUpdatedBy(resolveUserId());

        SampleInwardRegister saved = sampleInwardRepository.save(fresh);

        // Mark the source AR as completed (it's been superseded)
        source.setArStatusEnum(ArStatus.COMPLETED);
        source.setUpdatedBy(resolveUserId());
        sampleInwardRepository.save(source);

        log.info("Fresh AR {} generated from For_Information AR {}. Cross-reference set.",
                saved.getArNumber(), forInfoArNumber);
        log.info("[EMAIL-STUB] Fresh AR notification would be sent for AR {} (from {})",
                saved.getArNumber(), forInfoArNumber);

        return saved;
    }

    // ========================================================================
    // TT-03: Get linked AR chain (For_Information -> Fresh AR)
    // ========================================================================

    @Transactional(readOnly = true)
    public SampleInwardRegister getForInfoAr(String arNumber) {
        return sampleInwardRepository
                .findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("SampleInwardRegister", arNumber));
    }

    /**
     * List all ARs currently in FOR_INFORMATION status.
     */
    @Transactional(readOnly = true)
    public List<SampleInwardRegister> listForInfoArs() {
        return sampleInwardRepository.findByArStatus(ArStatus.FOR_INFORMATION.getDbValue());
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Resolve the current user ID from SecurityContextHolder.
     */
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        log.warn("Could not resolve user ID from SecurityContext — defaulting to 1");
        return 1L;
    }
}
