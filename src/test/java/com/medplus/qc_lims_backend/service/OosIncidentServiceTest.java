package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.OosIncidentRegister;
import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.AuthorizeRetestRequest;
import com.medplus.qc_lims_backend.dto.request.CompleteRetestRequest;
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
import com.medplus.qc_lims_backend.service.impl.OosIncidentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OosIncidentService covering OOS lifecycle operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OosIncidentServiceTest {

    @Mock private OosIncidentRepository oosIncidentRepository;
    @Mock private SampleInwardRepository sampleInwardRepository;
    @Mock private TestExecutionRepository testExecutionRepository;
    @Mock private ArSequenceGenerator sequenceGenerator;

    @InjectMocks private OosIncidentServiceImpl oosIncidentService;

    private OosIncidentRegister pendingIncident;

    @BeforeEach
    void setUp() {
        pendingIncident = new OosIncidentRegister();
        pendingIncident.setId(1L);
        pendingIncident.setIncidentNumber("OOS-260001");
        pendingIncident.setArNumber("MHSL260001");
        pendingIncident.setExecutionId(1);
        pendingIncident.setProductName("Paracetamol 500mg");
        pendingIncident.setProductId(100);
        pendingIncident.setBatchNo("BATCH-001");
        pendingIncident.setTestCode("ASSAY");
        pendingIncident.setSpecificationReference("98.0-102.0");
        pendingIncident.setObservedValue("95.5");
        pendingIncident.setUnitOfMeasurement("%");
        pendingIncident.setTestMethodName("USP");
        pendingIncident.setDeviationDescription("Result below specification");
        pendingIncident.setOosStatus("Pending");
        pendingIncident.setFinalDisposition("Pending");
        pendingIncident.setRetestAttemptCount(1);
        pendingIncident.setRaisedByUserId(1);
        pendingIncident.setStatus(RecordStatus.ACTIVE);
        pendingIncident.setCreatedAt(Instant.now());
        pendingIncident.setUpdatedAt(Instant.now());

        when(oosIncidentRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(pendingIncident));
        when(sequenceGenerator.generateOosNumber()).thenReturn("OOS-260001");
    }

    // --- OIS-01: Authorize Retest ---

    @Test
    void authorizeRetest_pendingToAuthorized_success() {
        when(oosIncidentRepository.save(any(OosIncidentRegister.class))).thenReturn(pendingIncident);

        AuthorizeRetestRequest request = new AuthorizeRetestRequest(
                "Investigation summary", "Root cause found", "Corrective action");

        OosIncidentResponse response = oosIncidentService.authorizeRetest(1L, request);

        assertNotNull(response);
        assertEquals("Retest_Authorized", pendingIncident.getFinalDisposition());
        assertEquals("Investigating", pendingIncident.getOosStatus());
        assertEquals("Investigation summary", pendingIncident.getInvestigationSummary());
        assertEquals("Root cause found", pendingIncident.getRootCause());
        verify(oosIncidentRepository).save(pendingIncident);
    }

    @Test
    void authorizeRetest_notPending_throws() {
        pendingIncident.setFinalDisposition("Retest_Passed");
        when(oosIncidentRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(pendingIncident));

        assertThrows(BusinessRuleViolationException.class,
                () -> oosIncidentService.authorizeRetest(1L, new AuthorizeRetestRequest(null, null, null)));
    }

    @Test
    void authorizeRetest_notFound_throws() {
        when(oosIncidentRepository.findByIdAndStatus(999L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> oosIncidentService.authorizeRetest(999L, new AuthorizeRetestRequest(null, null, null)));
    }

    // --- OIS-02: Complete Retest ---

    @Test
    void completeRetest_authorizedToPassed_success() {
        pendingIncident.setFinalDisposition("Retest_Authorized");

        when(oosIncidentRepository.save(any(OosIncidentRegister.class))).thenReturn(pendingIncident);

        CompleteRetestRequest request = new CompleteRetestRequest(
                "99.5", "%", "Retest passed", "Lab error", "Calibration performed");

        OosIncidentResponse response = oosIncidentService.completeRetest(1L, request);

        assertNotNull(response);
        assertEquals("Retest_Passed", pendingIncident.getFinalDisposition());
        assertEquals("Resolved", pendingIncident.getOosStatus());
        assertEquals("99.5", pendingIncident.getRevisedResult());
        assertEquals("%", pendingIncident.getRevisedResultUnit());
        assertNotNull(pendingIncident.getRetestCompletedDate());
        verify(oosIncidentRepository).save(pendingIncident);
    }

    @Test
    void completeRetest_notAuthorized_throws() {
        pendingIncident.setFinalDisposition("Pending");

        assertThrows(BusinessRuleViolationException.class,
                () -> oosIncidentService.completeRetest(1L,
                        new CompleteRetestRequest("99.5", "%", null, null, null)));
    }

    // --- OIS-03: Reject Batch ---

    @Test
    void rejectBatch_authorizedToRejected_success() {
        pendingIncident.setFinalDisposition("Retest_Authorized");
        SampleInwardRegister inward = new SampleInwardRegister();
        inward.setId(1L);
        inward.setArNumber("MHSL260001");
        inward.setStatus(RecordStatus.ACTIVE);
        when(sampleInwardRepository.findByArNumberAndStatus("MHSL260001", RecordStatus.ACTIVE))
                .thenReturn(Optional.of(inward));

        when(oosIncidentRepository.save(any(OosIncidentRegister.class))).thenReturn(pendingIncident);
        when(sampleInwardRepository.save(any(SampleInwardRegister.class))).thenReturn(inward);

        RejectBatchRequest request = new RejectBatchRequest(
                "Batch failed", "Raw material issue", "Vendor complaint raised");

        OosIncidentResponse response = oosIncidentService.rejectBatch(1L, request);

        assertNotNull(response);
        assertEquals("Batch_Rejected", pendingIncident.getFinalDisposition());
        assertEquals("Resolved", pendingIncident.getOosStatus());
        assertEquals(ArStatus.REJECTED, inward.getArStatusEnum());
        verify(sampleInwardRepository).save(inward);
    }

    @Test
    void rejectBatch_notAuthorized_throws() {
        pendingIncident.setFinalDisposition("Pending");

        assertThrows(BusinessRuleViolationException.class,
                () -> oosIncidentService.rejectBatch(1L,
                        new RejectBatchRequest("Reason", null, null)));
    }

    // --- OIS-05: Get Incident ---

    @Test
    void getIncident_success() {
        OosIncidentResponse response = oosIncidentService.getIncident(1L);

        assertNotNull(response);
        assertEquals("OOS-260001", response.incidentNumber());
        assertEquals("MHSL260001", response.arNumber());
        assertEquals("Pending", response.oosStatus());
    }

    @Test
    void getIncident_notFound_throws() {
        when(oosIncidentRepository.findByIdAndStatus(999L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> oosIncidentService.getIncident(999L));
    }

    // --- OIS-06: List by AR ---

    @Test
    void getIncidentsByAr_returnsList() {
        when(oosIncidentRepository.findByArNumberAndStatusOrderByCreatedAtDesc(
                "MHSL260001", RecordStatus.ACTIVE)).thenReturn(List.of(pendingIncident));

        var result = oosIncidentService.getIncidentsByAr("MHSL260001");

        assertEquals(1, result.size());
        assertEquals("OOS-260001", result.get(0).incidentNumber());
    }

    // --- OIS-04: List with filters ---

    @Test
    void listIncidents_withFilters_appliesFilters() {
        pendingIncident.setOosStatus("Pending");
        pendingIncident.setFinalDisposition("Pending");

        OosIncidentRegister resolved = new OosIncidentRegister();
        resolved.setId(2L);
        resolved.setIncidentNumber("OOS-260002");
        resolved.setArNumber("MHSL260001");
        resolved.setOosStatus("Resolved");
        resolved.setFinalDisposition("Retest_Passed");
        resolved.setStatus(RecordStatus.ACTIVE);
        resolved.setCreatedAt(Instant.now());

        when(oosIncidentRepository.findByStatus(RecordStatus.ACTIVE))
                .thenReturn(List.of(pendingIncident, resolved));

        var page = oosIncidentService.listIncidents(null, "Pending", null, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    // --- Event handling ---

    @Test
    void handleOosEvent_createsIncident() {
        when(oosIncidentRepository.save(any(OosIncidentRegister.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TestResultOosEvent event = new TestResultOosEvent(
                this, 1L, "MHSL260001", "ASSAY", "Paracetamol", 100, "BATCH-001",
                "98.0-102.0", "95.5", "%", "USP", 1L);

        oosIncidentService.handleOosEvent(event);

        ArgumentCaptor<OosIncidentRegister> captor = ArgumentCaptor.forClass(OosIncidentRegister.class);
        verify(oosIncidentRepository).save(captor.capture());

        OosIncidentRegister saved = captor.getValue();
        assertEquals("MHSL260001", saved.getArNumber());
        assertEquals("ASSAY", saved.getTestCode());
        assertEquals("95.5", saved.getObservedValue());
        assertEquals("Pending", saved.getFinalDisposition());
    }

    @Test
    void handleOosEvent_exceptionDoesNotPropagate() {
        when(oosIncidentRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        TestResultOosEvent event = new TestResultOosEvent(
                this, 1L, "MHSL260001", "ASSAY", "Paracetamol", 100, "BATCH-001",
                "98.0-102.0", "95.5", "%", "USP", 1L);

        // Should not throw — OOS creation failure must not block result recording
        assertDoesNotThrow(() -> oosIncidentService.handleOosEvent(event));
    }
}
