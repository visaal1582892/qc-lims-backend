package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.OosIncidentRegister;
import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.dto.request.AuthorizeRetestRequest;
import com.medplus.qc_lims_backend.dto.request.CompleteRetestRequest;
import com.medplus.qc_lims_backend.dto.request.OosInvestigationRequest;
import com.medplus.qc_lims_backend.dto.request.RaiseOosIncidentRequest;
import com.medplus.qc_lims_backend.dto.request.RejectBatchRequest;
import com.medplus.qc_lims_backend.dto.response.OosIncidentResponse;
import com.medplus.qc_lims_backend.enums.ArStatus;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.event.TestResultOosEvent;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.OosIncidentRepository;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OOS Incident Management service (SRS Module 8).
 *
 * Responsibilities:
 * - Auto-trigger OOS creation via TestResultOosEvent (decoupled from TES)
 * - OIS-01: Authorize retest (QC Head)
 * - OIS-02: Complete retest
 * - OIS-03: Reject batch
 * - OIS-04: List incidents (with filters)
 * - OIS-05: Get incident detail with retest chain
 * - OIS-06: List incidents by AR
 *
 * Email notifications are stubbed with logging (no spring-boot-starter-mail).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OosIncidentService {

    private final OosIncidentRepository oosIncidentRepository;
    private final SampleInwardRepository sampleInwardRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final ArSequenceGenerator sequenceGenerator;

    // ========================================================================
    // Auto-trigger via domain event (R10 — breaks TES <-> OIS bidirectional dependency)
    // ========================================================================

    /**
     * Listen for OOS events published by TestExecutionService.
     * Runs synchronously within the same transaction so rollback cascades.
     */
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT)
    public void handleOosEvent(TestResultOosEvent event) {
        try {
            log.info("OOS auto-trigger: arNumber={}, testCode={}", event.getArNumber(), event.getTestCode());
            OosIncidentRegister incident = new OosIncidentRegister();
            incident.setIncidentNumber(sequenceGenerator.generateOosNumber());
            incident.setArNumber(event.getArNumber());
            incident.setExecutionId(event.getTestExecutionId().intValue());
            incident.setProductName(event.getProductName());
            incident.setProductId(event.getProductId());
            incident.setBatchNo(event.getBatchNo());
            incident.setTestCode(event.getTestCode());
            incident.setSpecificationReference(event.getSpecificationReference());
            incident.setObservedValue(event.getObservedValue());
            incident.setUnitOfMeasurement(event.getUnitOfMeasurement());
            incident.setTestMethodName(event.getTestMethodName());
            incident.setDeviationDescription("Auto-triggered OOS: result out of specification");
            incident.setOosStatus("Pending");
            incident.setFinalDisposition("Pending");
            incident.setRetestAttemptCount(1);
            incident.setRaisedByUserId(event.getCreatedByUserId().intValue());
            incident.setStatus(RecordStatus.ACTIVE);

            Long userId = event.getCreatedByUserId();
            if (userId != null) {
                incident.setCreatedBy(userId);
                incident.setUpdatedBy(userId);
            }

            oosIncidentRepository.save(incident);
            log.info("OOS incident created: {}", incident.getIncidentNumber());

            // Stub email notification (logging only)
            log.info("[EMAIL-STUB] OOS notification would be sent for incident {} on AR {}",
                    incident.getIncidentNumber(), incident.getArNumber());
        } catch (Exception e) {
            // Log but do not rethrow — OOS creation failure should not block result recording
            log.error("Failed to auto-create OOS incident for arNumber={}, testExecutionId={}: {}",
                    event.getArNumber(), event.getTestExecutionId(), e.getMessage(), e);
        }
    }

    // ========================================================================
    // OIS-01: Authorize Retest (QC Head)
    // ========================================================================

    @Transactional
    public OosIncidentResponse authorizeRetest(Long incidentId, AuthorizeRetestRequest request) {
        OosIncidentRegister incident = oosIncidentRepository
                .findByIdAndStatus(incidentId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("OosIncidentRegister", incidentId));

        if (!"Pending".equals(incident.getFinalDisposition())) {
            throw new BusinessRuleViolationException(
                    "Cannot authorize retest: incident is already " + incident.getFinalDisposition());
        }

        incident.setFinalDisposition("Retest_Authorized");
        incident.setOosStatus("Investigating");
        if (request.investigationSummary() != null) {
            incident.setInvestigationSummary(request.investigationSummary());
        }
        if (request.rootCause() != null) {
            incident.setRootCause(request.rootCause());
        }
        if (request.correctiveAction() != null) {
            incident.setCorrectiveAction(request.correctiveAction());
        }
        incident.setUpdatedBy(resolveUserId());

        OosIncidentRegister saved = oosIncidentRepository.save(incident);

        // Stub email notification
        log.info("[EMAIL-STUB] Retest authorization notification for incident {} on AR {}",
                saved.getIncidentNumber(), saved.getArNumber());

        return OosIncidentResponse.fromEntity(saved);
    }

    // ========================================================================
    // OIS-02: Complete Retest
    // ========================================================================

    @Transactional
    public OosIncidentResponse completeRetest(Long incidentId, CompleteRetestRequest request) {
        OosIncidentRegister incident = oosIncidentRepository
                .findByIdAndStatus(incidentId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("OosIncidentRegister", incidentId));

        if (!"Retest_Authorized".equals(incident.getFinalDisposition())) {
            throw new BusinessRuleViolationException(
                    "Cannot complete retest: incident disposition is " + incident.getFinalDisposition());
        }

        incident.setFinalDisposition("Retest_Passed");
        incident.setOosStatus("Resolved");
        incident.setRevisedResult(request.revisedResult());
        incident.setRevisedResultUnit(request.revisedResultUnit());
        incident.setRetestCompletedDate(LocalDateTime.now());
        if (request.investigationSummary() != null) {
            incident.setInvestigationSummary(request.investigationSummary());
        }
        if (request.rootCause() != null) {
            incident.setRootCause(request.rootCause());
        }
        if (request.correctiveAction() != null) {
            incident.setCorrectiveAction(request.correctiveAction());
        }
        incident.setUpdatedBy(resolveUserId());

        OosIncidentRegister saved = oosIncidentRepository.save(incident);

        // Stub email notification
        log.info("[EMAIL-STUB] Retest completion notification for incident {} on AR {}",
                saved.getIncidentNumber(), saved.getArNumber());

        return OosIncidentResponse.fromEntity(saved);
    }

    // ========================================================================
    // OIS-03: Reject Batch
    // ========================================================================

    @Transactional
    public OosIncidentResponse rejectBatch(Long incidentId, RejectBatchRequest request) {
        OosIncidentRegister incident = oosIncidentRepository
                .findByIdAndStatus(incidentId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("OosIncidentRegister", incidentId));

        if (!"Retest_Authorized".equals(incident.getFinalDisposition())) {
            throw new BusinessRuleViolationException(
                    "Cannot reject batch: incident disposition is " + incident.getFinalDisposition());
        }

        incident.setFinalDisposition("Batch_Rejected");
        incident.setOosStatus("Resolved");
        if (request.investigationSummary() != null) {
            incident.setInvestigationSummary(request.investigationSummary());
        }
        if (request.rootCause() != null) {
            incident.setRootCause(request.rootCause());
        }
        if (request.correctiveAction() != null) {
            incident.setCorrectiveAction(request.correctiveAction());
        }
        incident.setUpdatedBy(resolveUserId());

        OosIncidentRegister saved = oosIncidentRepository.save(incident);

        // Mark the source AR as REJECTED
        SampleInwardRegister inward = sampleInwardRepository
                .findByArNumberAndStatus(incident.getArNumber(), RecordStatus.ACTIVE)
                .orElse(null);
        if (inward != null) {
            inward.setArStatusEnum(ArStatus.REJECTED);
            sampleInwardRepository.save(inward);
        }

        // Stub email notification
        log.info("[EMAIL-STUB] Batch rejection notification for incident {} on AR {}",
                saved.getIncidentNumber(), saved.getArNumber());

        return OosIncidentResponse.fromEntity(saved);
    }

    // ========================================================================
    // OIS-04: List OOS Incidents (paginated, with filters)
    // ========================================================================

    @Transactional(readOnly = true)
    public Page<OosIncidentResponse> listIncidents(String arNumber, String status,
                                                    String disposition, Pageable pageable) {
        List<OosIncidentRegister> source;
        if (arNumber != null && !arNumber.isBlank()) {
            source = oosIncidentRepository.findByArNumberAndStatusOrderByCreatedAtDesc(
                    arNumber, RecordStatus.ACTIVE);
        } else {
            source = oosIncidentRepository.findByStatus(RecordStatus.ACTIVE);
        }

        List<OosIncidentRegister> filtered = source.stream()
                .filter(o -> status == null || status.isBlank() || status.equalsIgnoreCase(o.getOosStatus()))
                .filter(o -> disposition == null || disposition.isBlank() || disposition.equalsIgnoreCase(o.getFinalDisposition()))
                .toList();

        long total = filtered.size();
        List<OosIncidentRegister> page = filtered.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                page.stream().map(OosIncidentResponse::fromEntity).toList(),
                pageable,
                total);
    }

    // ========================================================================
    // OIS-05: Get incident detail with retest chain
    // ========================================================================

    @Transactional(readOnly = true)
    public OosIncidentResponse getIncident(Long incidentId) {
        OosIncidentRegister incident = oosIncidentRepository
                .findByIdAndStatus(incidentId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("OosIncidentRegister", incidentId));
        return OosIncidentResponse.fromEntity(incident);
    }

    // ========================================================================
    // OIS-06: List incidents by AR number
    // ========================================================================

    @Transactional(readOnly = true)
    public List<OosIncidentResponse> getIncidentsByAr(String arNumber) {
        return oosIncidentRepository
                .findByArNumberAndStatusOrderByCreatedAtDesc(arNumber, RecordStatus.ACTIVE)
                .stream()
                .map(OosIncidentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // OIS-07: Manually raise an OOS incident
    // ========================================================================

    @Transactional
    public OosIncidentResponse raiseOosIncident(RaiseOosIncidentRequest request) {
        // Validate execution exists
        TestExecution execution = testExecutionRepository
                .findByIdAndStatus(Long.valueOf(request.getExecutionId()), RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestExecution", request.getExecutionId()));

        // Check for duplicate: active incident already exists for this execution
        List<OosIncidentRegister> existing = oosIncidentRepository
                .findByExecutionIdAndStatus(request.getExecutionId(), RecordStatus.ACTIVE);
        if (!existing.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "OOS incident already exists for execution ID " + request.getExecutionId());
        }

        OosIncidentRegister incident = new OosIncidentRegister();
        incident.setIncidentNumber(sequenceGenerator.generateOosNumber());
        incident.setArNumber(request.getArNumber());
        incident.setExecutionId(request.getExecutionId());
        incident.setProductId(request.getProductId());
        incident.setProductName(request.getProductName());
        incident.setBatchNo(request.getBatchNo());
        incident.setTestCode(request.getTestCode());
        incident.setSpecificationReference(request.getSpecificationReference());
        incident.setObservedValue(request.getObservedValue());
        incident.setUnitOfMeasurement(request.getUnitOfMeasurement());
        incident.setTestMethodName(request.getTestMethodName());
        incident.setDeviationDescription(request.getDeviationDescription() != null
                ? request.getDeviationDescription() : "Manually raised OOS");
        incident.setOosStatus("Pending");
        incident.setFinalDisposition("Pending");
        incident.setRetestAttemptCount(1);
        incident.setRaisedByUserId(request.getExecutionId()); // Use execution's analyst
        incident.setStatus(RecordStatus.ACTIVE);
        incident.setCreatedBy(resolveUserId());
        incident.setUpdatedBy(resolveUserId());

        OosIncidentRegister saved = oosIncidentRepository.save(incident);

        log.info("OOS incident manually raised: {}, AR: {}, execution: {}",
                saved.getIncidentNumber(), incident.getArNumber(), request.getExecutionId());

        return OosIncidentResponse.fromEntity(saved);
    }

    // ========================================================================
    // OIS-08: Submit investigation findings
    // ========================================================================

    @Transactional
    public OosIncidentResponse investigate(Long incidentId, OosInvestigationRequest request) {
        OosIncidentRegister incident = oosIncidentRepository
                .findByIdAndStatus(incidentId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("OosIncidentRegister", incidentId));

        if (!"Pending".equals(incident.getFinalDisposition())
                && !"Retest_Authorized".equals(incident.getFinalDisposition())) {
            throw new BusinessRuleViolationException(
                    "Cannot investigate: incident disposition is " + incident.getFinalDisposition());
        }

        if (request.getInvestigationSummary() != null) {
            incident.setInvestigationSummary(request.getInvestigationSummary());
        }
        if (request.getRootCause() != null) {
            incident.setRootCause(request.getRootCause());
        }
        if (request.getCorrectiveAction() != null) {
            incident.setCorrectiveAction(request.getCorrectiveAction());
        }
        incident.setUpdatedBy(resolveUserId());

        OosIncidentRegister saved = oosIncidentRepository.save(incident);

        log.info("OOS investigation submitted: {}", saved.getIncidentNumber());

        return OosIncidentResponse.fromEntity(saved);
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Resolve the current user ID from SecurityContextHolder (consistent with existing services).
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
