package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.CreateInwardRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateInwardRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.InwardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Sample Inward Registration (Module 2).
 *
 * Handles AR number generation, inward CRUD, and soft-delete.
 */
@RestController
@RequestMapping("/api/inward")
@RequiredArgsConstructor
public class InwardController {

    private final InwardService inwardService;

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_VIEW + "')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InwardResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(inwardService.listAll()));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_VIEW + "')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InwardResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inwardService.getById(id)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_VIEW + "')")
    @GetMapping("/search/batch")
    public ResponseEntity<ApiResponse<List<InwardResponse>>> searchByBatch(@RequestParam String batchNo) {
        return ResponseEntity.ok(ApiResponse.ok(inwardService.searchByBatchNo(batchNo)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_CREATE + "')")
    @PostMapping
    public ResponseEntity<ApiResponse<InwardResponse>> create(@Valid @RequestBody CreateInwardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inwardService.create(request, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_UPDATE + "')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InwardResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInwardRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inwardService.update(id, request, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_INWARD_DELETE + "')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inwardService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
