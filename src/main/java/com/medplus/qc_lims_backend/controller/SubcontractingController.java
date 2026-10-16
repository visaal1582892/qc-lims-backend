package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.CreateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateSubcontractRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.SubcontractResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.SubcontractingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Subcontracting controller (SRS Module 10).
 *
 * Endpoints:
 * - POST   /api/v1/subcontracts                — Create subcontract entry (SC-01)
 * - GET    /api/v1/subcontracts                — List all subcontracts (SC-02)
 * - GET    /api/v1/subcontracts/{id}           — Get subcontract detail (SC-03)
 * - PUT    /api/v1/subcontracts/{id}           — Update subcontract (SC-04)
 * - PATCH  /api/v1/subcontracts/{id}/roa-received — Mark ROA received (SC-05)
 */
@RestController
@RequestMapping("/api/v1/subcontracts")
@RequiredArgsConstructor
public class SubcontractingController {

    private final SubcontractingService subcontractingService;

    /**
     * SC-01: Create a subcontract entry.
     * Dispatches a sample to an external lab.
     * NO AR Number is generated (ar_number remains NULL).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUBCONTRACT_CREATE')")
    public ResponseEntity<ApiResponse<SubcontractResponse>> createSubcontract(
            @Valid @RequestBody CreateSubcontractRequest request) {
        SubcontractResponse response = subcontractingService.createSubcontract(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * SC-02: List all subcontract register entries.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUBCONTRACT_VIEW')")
    public ResponseEntity<ApiResponse<List<SubcontractResponse>>> listSubcontracts() {
        List<SubcontractResponse> subcontracts = subcontractingService.listSubcontracts();
        return ResponseEntity.ok(ApiResponse.ok(subcontracts));
    }

    /**
     * SC-03: Get subcontract detail by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUBCONTRACT_VIEW')")
    public ResponseEntity<ApiResponse<SubcontractResponse>> getSubcontract(@PathVariable Long id) {
        SubcontractResponse response = subcontractingService.getSubcontract(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * SC-04: Update subcontract remarks/status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUBCONTRACT_UPDATE')")
    public ResponseEntity<ApiResponse<SubcontractResponse>> updateSubcontract(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubcontractRequest request) {
        SubcontractResponse response = subcontractingService.updateSubcontract(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * SC-05: Mark ROA as received for a subcontract.
     */
    @PatchMapping("/{id}/roa-received")
    @PreAuthorize("hasAuthority('ROLE_SUBCONTRACT_UPDATE')")
    public ResponseEntity<ApiResponse<SubcontractResponse>> markRoaReceived(@PathVariable Long id) {
        SubcontractResponse response = subcontractingService.markRoaReceived(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
