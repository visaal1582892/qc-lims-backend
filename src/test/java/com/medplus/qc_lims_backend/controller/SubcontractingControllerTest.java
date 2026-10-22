package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.CreateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.SubcontractResponse;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.service.SubcontractingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T-18: SubcontractingController — verifies thin controller delegates to service
 * and returns correct HTTP status codes and ApiResponse envelopes.
 */
class SubcontractingControllerTest {

    private final SubcontractingService service = mock(SubcontractingService.class);
    private final SubcontractingController controller = new SubcontractingController(service);

    private SubcontractResponse makeResponse() {
        return new SubcontractResponse(
                1L, "SC-20261016-ABC", "TRF-001",
                "AR-001", "External Lab", "Equipment breakdown",
                LocalDate.now(), 1L, null, "DISPATCHED", "remark", false,
                Instant.now(), Instant.now());
    }

    // --- GET listAll ---

    @Test
    void listSubcontractsReturns200WithApiResponse() {
        when(service.listSubcontracts()).thenReturn(List.of(makeResponse()));

        ResponseEntity<ApiResponse<List<SubcontractResponse>>> response = controller.listSubcontracts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals(1, response.getBody().data().size());
    }

    @Test
    void listSubcontractsDelegatesToService() {
        when(service.listSubcontracts()).thenReturn(List.of());
        controller.listSubcontracts();
        verify(service).listSubcontracts();
    }

    // --- GET getById ---

    @Test
    void getByIdReturns200() {
        when(service.getSubcontract(1L)).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<SubcontractResponse>> response = controller.getSubcontract(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SC-20261016-ABC", response.getBody().data().subcontractNo());
    }

    @Test
    void getByIdNotFoundPropagates404() {
        when(service.getSubcontract(99L))
                .thenThrow(new EntityNotFoundException("SubcontractRegister", 99L));

        assertThrows(EntityNotFoundException.class, () -> controller.getSubcontract(99L));
    }

    // --- POST create (201 Created) ---

    @Test
    void createReturns201() {
        CreateSubcontractRequest req = new CreateSubcontractRequest(
                null, "Lab Corp", "Breakdown", 1L, null);

        when(service.createSubcontract(any(CreateSubcontractRequest.class))).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<SubcontractResponse>> response = controller.createSubcontract(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().success());
    }

    // --- PUT update (200 OK) ---

    @Test
    void updateReturns200() {
        UpdateSubcontractRequest req = new UpdateSubcontractRequest("Updated remarks", "IN_PROGRESS");
        when(service.updateSubcontract(eq(1L), any(UpdateSubcontractRequest.class)))
                .thenReturn(makeResponse());

        ResponseEntity<ApiResponse<SubcontractResponse>> response = controller.updateSubcontract(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- POST mark-roa (200 OK) ---

    @Test
    void markRoaReceivedReturns200() {
        SubcontractResponse roaResponse = new SubcontractResponse(
                1L, "SC-20261016-ABC", "TRF-001",
                "AR-001", "External Lab", "Equipment breakdown",
                LocalDate.now(), 1L, null, "COMPLETED", "remark", true,
                Instant.now(), Instant.now());

        when(service.markRoaReceived(1L)).thenReturn(roaResponse);

        ResponseEntity<ApiResponse<SubcontractResponse>> response = controller.markRoaReceived(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Business rule violation propagation ---

    @Test
    void createBusinessRuleViolationPropagates() {
        CreateSubcontractRequest req = new CreateSubcontractRequest(
                null, null, null, 1L, null);

        when(service.createSubcontract(any(CreateSubcontractRequest.class)))
                .thenThrow(new BusinessRuleViolationException("Missing external lab name"));

        assertThrows(BusinessRuleViolationException.class, () -> controller.createSubcontract(req));
    }

    @Test
    void updateBusinessRuleViolationPropagates() {
        UpdateSubcontractRequest req = new UpdateSubcontractRequest(null, "INVALID");

        when(service.updateSubcontract(eq(1L), any(UpdateSubcontractRequest.class)))
                .thenThrow(new BusinessRuleViolationException("Invalid transition"));

        assertThrows(BusinessRuleViolationException.class, () -> controller.updateSubcontract(1L, req));
    }

    // --- ApiResponse envelope consistency ---

    @Test
    void allResponsesUseApiResponseEnvelope() {
        when(service.listSubcontracts()).thenReturn(List.of(makeResponse()));

        var response = controller.listSubcontracts();
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertNotNull(response.getBody().timestamp());
        assertEquals("Success", response.getBody().message());
    }
}
