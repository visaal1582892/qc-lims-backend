package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.ArLifecycleRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.service.InwardService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InwardController AR Lifecycle endpoints (Phase 5).
 *
 * Test matrix:
 *   T-AR-C01: closeAr returns 200 with ApiResponse envelope
 *   T-AR-C02: closeAr propagates BusinessRuleViolationException
 *   T-AR-C03: closeAr propagates EntityNotFoundException
 *   T-AR-C04: cancelAr returns 200 with ApiResponse envelope
 *   T-AR-C05: cancelAr propagates BusinessRuleViolationException
 *   T-AR-C06: cancelAr propagates EntityNotFoundException
 *   T-AR-C07: listByStatus returns 200
 *   T-AR-C08: controller delegates to service for all AR lifecycle ops
 */
class InwardControllerArLifecycleTest {

    private final InwardService service = mock(InwardService.class);
    private final InwardController controller = new InwardController(service);

    private InwardResponse makeInwardResponse() {
        return new InwardResponse(
                1L, "MHSL260001", 1, 100, null, "BATCH-001",
                10, 100, "Refrigerator", "ALL_TESTS_COMPLETE", null,
                false, null, null, LocalDate.of(2026, 1, 15),
                null, null, null);
    }

    // ==================== closeAr ====================

    @Test
    void closeArReturns200WithApiResponse() {
        // T-AR-C01
        when(service.closeAr(eq(1L), any(ArLifecycleRequest.class), any()))
                .thenReturn(makeInwardResponse());

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("All tests complete");

        ResponseEntity<ApiResponse<InwardResponse>> response = controller.closeAr(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("ALL_TESTS_COMPLETE", response.getBody().data().arStatus());
        verify(service).closeAr(1L, request, null);
    }

    @Test
    void closeArPropagatesBusinessRuleViolationException() {
        // T-AR-C02
        when(service.closeAr(eq(1L), any(ArLifecycleRequest.class), any()))
                .thenThrow(new BusinessRuleViolationException("Cannot close AR"));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Close");

        assertThrows(BusinessRuleViolationException.class,
                () -> controller.closeAr(1L, request));
    }

    @Test
    void closeArPropagatesEntityNotFoundException() {
        // T-AR-C03
        when(service.closeAr(eq(99L), any(ArLifecycleRequest.class), any()))
                .thenThrow(new EntityNotFoundException("InwardRegistration", 99L));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Close");

        assertThrows(EntityNotFoundException.class,
                () -> controller.closeAr(99L, request));
    }

    // ==================== cancelAr ====================

    @Test
    void cancelArReturns200WithApiResponse() {
        // T-AR-C04
        when(service.cancelAr(eq(1L), any(ArLifecycleRequest.class), any()))
                .thenReturn(makeInwardResponse());

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Project cancelled");

        ResponseEntity<ApiResponse<InwardResponse>> response = controller.cancelAr(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        verify(service).cancelAr(1L, request, null);
    }

    @Test
    void cancelArPropagatesBusinessRuleViolationException() {
        // T-AR-C05
        when(service.cancelAr(eq(1L), any(ArLifecycleRequest.class), any()))
                .thenThrow(new BusinessRuleViolationException("Cannot cancel AR"));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Cancel");

        assertThrows(BusinessRuleViolationException.class,
                () -> controller.cancelAr(1L, request));
    }

    @Test
    void cancelArPropagatesEntityNotFoundException() {
        // T-AR-C06
        when(service.cancelAr(eq(99L), any(ArLifecycleRequest.class), any()))
                .thenThrow(new EntityNotFoundException("InwardRegistration", 99L));

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Cancel");

        assertThrows(EntityNotFoundException.class,
                () -> controller.cancelAr(99L, request));
    }

    // ==================== listByStatus ====================

    @Test
    void listByStatusReturns200() {
        // T-AR-C07
        when(service.listByArStatus("ALL_TESTS_COMPLETE"))
                .thenReturn(java.util.List.of(makeInwardResponse()));

        ResponseEntity<ApiResponse<java.util.List<InwardResponse>>> response = controller.listByStatus("ALL_TESTS_COMPLETE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
        verify(service).listByArStatus("ALL_TESTS_COMPLETE");
    }

    // ==================== Controller is thin ====================

    @Test
    void controllerDelegatesAllArLifecycleOps() {
        // T-AR-C08
        when(service.closeAr(eq(1L), any(), any())).thenReturn(makeInwardResponse());
        when(service.cancelAr(eq(2L), any(), any())).thenReturn(makeInwardResponse());

        ArLifecycleRequest request = new ArLifecycleRequest();
        request.setReason("Test reason");

        controller.closeAr(1L, request);
        controller.cancelAr(2L, request);

        verify(service).closeAr(1L, request, null);
        verify(service).cancelAr(2L, request, null);
    }
}
