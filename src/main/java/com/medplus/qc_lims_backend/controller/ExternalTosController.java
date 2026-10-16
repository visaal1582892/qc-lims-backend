package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.TOSTriggerRequest;
import com.medplus.qc_lims_backend.dto.request.TOSUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.TOSTriggerResponse;
import com.medplus.qc_lims_backend.dto.response.TOSUpdateResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.ExternalTosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for external TOS (Test Order Specification) hooks.
 *
 * Exposes two endpoints for external warehouse/ERP systems:
 * - POST /api/v1/external/tos-trigger  — Submit new GRN/Transfer Order
 * - POST /api/v1/external/tos-update   — Submit TOS status update callback
 *
 * Both endpoints are stubs with placeholder logic per SRS Module 14.1.
 * Responses are wrapped in ApiResponse<T> envelope consistent with
 * existing controller pattern.
 *
 * Security: C-4 — Uses Rights constant concatenation in @PreAuthorize.
 */
@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalTosController {

    private final ExternalTosService externalTosService;

    /**
     * POST /api/v1/external/tos-trigger
     *
     * Accepts a GRN/Transfer Order payload from an external system.
     * Returns 202 ACCEPTED with acknowledgment.
     *
     * @param request the incoming TOS trigger payload
     * @return 202 ACCEPTED with TOSTriggerResponse wrapped in ApiResponse
     */
    @PostMapping("/tos-trigger")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_EXTERNAL_TOS_TRIGGER + "')")
    public ResponseEntity<ApiResponse<TOSTriggerResponse>> tosTrigger(
            @Valid @RequestBody TOSTriggerRequest request) {

        TOSTriggerResponse response = externalTosService.processTosTrigger(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(response));
    }

    /**
     * POST /api/v1/external/tos-update
     *
     * Accepts a TOS status update callback from an external system.
     * Returns 200 OK with acknowledgment.
     *
     * @param request the incoming TOS update payload
     * @return 200 OK with TOSUpdateResponse wrapped in ApiResponse
     */
    @PostMapping("/tos-update")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_EXTERNAL_TOS_UPDATE + "')")
    public ResponseEntity<ApiResponse<TOSUpdateResponse>> tosUpdate(
            @Valid @RequestBody TOSUpdateRequest request) {

        TOSUpdateResponse response = externalTosService.processTosUpdate(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
