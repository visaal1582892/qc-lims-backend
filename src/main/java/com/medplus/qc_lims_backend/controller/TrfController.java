package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.TrfCreateRequest;
import com.medplus.qc_lims_backend.dto.request.TrfUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.TrfResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TrfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for e-TRF Management (Module 3).
 *
 * Handles CRUD, lifecycle transitions (accept/reject), and parameter assignment.
 */
@RestController
@RequestMapping("/api/trf")
@RequiredArgsConstructor
public class TrfController {

    private final TrfService trfService;

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_VIEW + "')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrfResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(trfService.listAll()));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_VIEW + "')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrfResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.getById(id)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_VIEW + "')")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<TrfResponse>>> listByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.listByStatus(status)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_VIEW + "')")
    @GetMapping("/search/grn")
    public ResponseEntity<ApiResponse<List<TrfResponse>>> searchByGrn(@RequestParam String grnNumber) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.searchByGrnNumber(grnNumber)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_CREATE + "')")
    @PostMapping
    public ResponseEntity<ApiResponse<TrfResponse>> create(@Valid @RequestBody TrfCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(trfService.create(request, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_UPDATE + "')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TrfResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TrfUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.update(id, request, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_DELETE + "')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        trfService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // --- Lifecycle transitions ---

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_ACCEPT + "')")
    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<TrfResponse>> accept(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.accept(id, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_REJECT + "')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TrfResponse>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(trfService.reject(id, null)));
    }

    // --- QC Head parameter assignment ---

    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRF_ASSIGN_PARAMETERS + "')")
    @PostMapping("/{id}/assign-parameters")
    public ResponseEntity<ApiResponse<Void>> assignParameters(
            @PathVariable Long id,
            @RequestBody List<Integer> parameterIds) {
        trfService.assignTestParameters(id, parameterIds, null);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
