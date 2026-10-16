package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SubcontractRegister;
import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.dto.request.CreateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.response.SubcontractResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.enums.SubcontractStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SubcontractRepository;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SubcontractingService covering SRS Module 10 acceptance criteria.
 *
 * AC-06: createSubcontract() records a subcontract entry
 * AC-07: NO AR Number generated for subcontracted samples
 * AC-08: Validation — trfId, externalLabName, reason, qcHeadId
 * AC-09: listSubcontracts()
 * AC-10: getByTrfId / getById
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubcontractingServiceTest {

    @Mock private SubcontractRepository subcontractRepository;
    @Mock private TrfRepository trfRepository;
    @InjectMocks private SubcontractingService subcontractingService;

    private Authentication mockAuth;

    @BeforeEach
    void setUpSecurityContext() {
        mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);
    }

    // =========================================================================
    // AC-06: Create Subcontract Entry
    // =========================================================================

    @Test
    void testCreateValidSubcontract() {
        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "External Lab Inc", "Equipment breakdown", 1L, LocalDate.now().plusDays(30));

        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> {
            SubcontractRegister saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SubcontractResponse response = subcontractingService.createSubcontract(request);

        assertNotNull(response);
        assertEquals("External Lab Inc", response.externalLabName());
        assertEquals("Equipment breakdown", response.reason());
        assertNull(response.arNumber(), "AR Number must be NULL for subcontracted samples");
        assertEquals(SubcontractStatus.DISPATCHED.getDbValue(), response.status());
        assertFalse(response.roaReceived());

        ArgumentCaptor<SubcontractRegister> captor = ArgumentCaptor.forClass(SubcontractRegister.class);
        verify(subcontractRepository).save(captor.capture());
        SubcontractRegister saved = captor.getValue();
        assertNull(saved.getArNumber());
        assertEquals(RecordStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getSubcontractNo());
        assertTrue(saved.getSubcontractNo().startsWith("SC-"));
    }

    @Test
    void testCreateWithTrfIdAndAcceptedStatus() {
        TrfMaster trf = new TrfMaster();
        trf.setId(1L);
        trf.setTrfStatus("Accepted");

        when(trfRepository.findById(1L)).thenReturn(Optional.of(trf));
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> {
            SubcontractRegister saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                1, "Lab Corp", "Maintenance window", 1L, null);

        SubcontractResponse response = subcontractingService.createSubcontract(request);

        assertNotNull(response);
        assertEquals(1, response.trfId());
        assertNull(response.arNumber());
    }

    // =========================================================================
    // AC-07: NO AR Number generated
    // =========================================================================

    @Test
    void testCreateNoArNumberGenerated() {
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> {
            SubcontractRegister saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "Test Lab", "Breakdown", 1L, null);

        SubcontractResponse response = subcontractingService.createSubcontract(request);

        assertNull(response.arNumber(), "AC-07: ar_number must remain NULL for subcontracted samples");
    }

    // =========================================================================
    // AC-08: Validation — mandatory fields
    // =========================================================================

    @Test
    void testCreateRejectsNullQcHeadId() {
        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "Lab", "Reason", null, null);

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    @Test
    void testCreateRejectsZeroQcHeadId() {
        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "Lab", "Reason", 0L, null);

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    @Test
    void testCreateRejectsNegativeQcHeadId() {
        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "Lab", "Reason", -1L, null);

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    @Test
    void testCreateRejectsNonAcceptedTrf() {
        TrfMaster trf = new TrfMaster();
        trf.setId(1L);
        trf.setTrfStatus("Rejected");

        when(trfRepository.findById(1L)).thenReturn(Optional.of(trf));

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                1, "Lab", "Reason", 1L, null);

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    @Test
    void testCreateRejectsPendingTrf() {
        TrfMaster trf = new TrfMaster();
        trf.setId(1L);
        trf.setTrfStatus("Pending_Feasibility");

        when(trfRepository.findById(1L)).thenReturn(Optional.of(trf));

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                1, "Lab", "Reason", 1L, null);

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    @Test
    void testCreateRejectsMissingTrf() {
        when(trfRepository.findById(1L)).thenReturn(Optional.empty());

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                1, "Lab", "Reason", 1L, null);

        assertThrows(EntityNotFoundException.class, () ->
                subcontractingService.createSubcontract(request));
    }

    // =========================================================================
    // AC-09: List Subcontracts
    // =========================================================================

    @Test
    void testListAll() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractNo("SC-20261016-ABC123");
        reg.setExternalLabName("Lab A");
        reg.setSubcontractStatus("DISPATCHED");
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findByStatusOrderByDispatchDateDesc(RecordStatus.ACTIVE))
                .thenReturn(List.of(reg));

        List<SubcontractResponse> result = subcontractingService.listSubcontracts();

        assertEquals(1, result.size());
        assertEquals("SC-20261016-ABC123", result.get(0).subcontractNo());
    }

    @Test
    void testListAllEmpty() {
        when(subcontractRepository.findByStatusOrderByDispatchDateDesc(RecordStatus.ACTIVE))
                .thenReturn(List.of());

        List<SubcontractResponse> result = subcontractingService.listSubcontracts();

        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // AC-10: Get Subcontract by ID
    // =========================================================================

    @Test
    void testGetById() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractNo("SC-20261016-XYZ");
        reg.setExternalLabName("Lab B");
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));

        SubcontractResponse response = subcontractingService.getSubcontract(1L);

        assertEquals("SC-20261016-XYZ", response.subcontractNo());
    }

    @Test
    void testGetByIdNotFound() {
        when(subcontractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                subcontractingService.getSubcontract(99L));
    }

    @Test
    void testGetByIdInactiveThrowsNotFound() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setStatus(RecordStatus.DELETED);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));

        assertThrows(EntityNotFoundException.class, () ->
                subcontractingService.getSubcontract(1L));
    }

    // =========================================================================
    // Update Subcontract (SC-04)
    // =========================================================================

    @Test
    void testUpdateRemarks() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractNo("SC-20261016-UPD");
        reg.setSubcontractStatus("DISPATCHED");
        reg.setStatus(RecordStatus.ACTIVE);
        reg.setRemarks(null);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSubcontractRequest request = new UpdateSubcontractRequest("New remark", null);
        SubcontractResponse response = subcontractingService.updateSubcontract(1L, request);

        assertNotNull(response);
        assertTrue(response.remarks().contains("New remark"));
    }

    @Test
    void testUpdateStatusTransition() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractStatus("DISPATCHED");
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSubcontractRequest request = new UpdateSubcontractRequest(null, "IN_PROGRESS");
        SubcontractResponse response = subcontractingService.updateSubcontract(1L, request);

        assertEquals("IN_PROGRESS", response.status());
    }

    @Test
    void testUpdateRejectsTerminalStatusTransition() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractStatus("COMPLETED");
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));

        UpdateSubcontractRequest request = new UpdateSubcontractRequest(null, "DISPATCHED");

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.updateSubcontract(1L, request));
    }

    @Test
    void testUpdateRejectsCancelledStatusTransition() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractStatus("CANCELLED");
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));

        UpdateSubcontractRequest request = new UpdateSubcontractRequest(null, "DISPATCHED");

        assertThrows(BusinessRuleViolationException.class, () ->
                subcontractingService.updateSubcontract(1L, request));
    }

    // =========================================================================
    // Mark ROA Received (SC-05)
    // =========================================================================

    @Test
    void testMarkRoaReceived() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractStatus("DISPATCHED");
        reg.setRoaReceived(false);
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcontractResponse response = subcontractingService.markRoaReceived(1L);

        assertTrue(response.roaReceived());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void testMarkRoaReceivedFromInProgress() {
        SubcontractRegister reg = new SubcontractRegister();
        reg.setId(1L);
        reg.setSubcontractStatus("IN_PROGRESS");
        reg.setRoaReceived(false);
        reg.setStatus(RecordStatus.ACTIVE);

        when(subcontractRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(subcontractRepository.save(any(SubcontractRegister.class))).thenAnswer(inv -> inv.getArgument(0));

        SubcontractResponse response = subcontractingService.markRoaReceived(1L);

        assertTrue(response.roaReceived());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void testCreateWithoutSecurityContext() {
        SecurityContextHolder.clearContext();

        CreateSubcontractRequest request = new CreateSubcontractRequest(
                null, "Lab", "Reason", 1L, null);

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                subcontractingService.createSubcontract(request));
    }
}
