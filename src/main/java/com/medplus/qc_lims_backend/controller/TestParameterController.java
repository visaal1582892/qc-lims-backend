package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.TestParameterRequest;
import com.medplus.qc_lims_backend.dto.request.TestParameterUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.TestParameterResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TestParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Test Parameter Master CRUD operations.
 *
 * All endpoints wrapped in ApiResponse<T> envelope.
 * Authorization via @PreAuthorize with Rights.java constants.
 * No business logic — delegates to TestParameterService.
 *
 * Security: C-4 — Uses Rights constant concatenation in @PreAuthorize.
 */
@RestController
@RequestMapping("/api/test-parameters")
@RequiredArgsConstructor
public class TestParameterController {

    private final TestParameterService service;

    // --- READ ---

    @GetMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_READ + "')")
    public ResponseEntity<ApiResponse<List<TestParameterResponse>>> list(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String standardReference) {

        List<TestParameterResponse> result;
        if (search != null && !search.isBlank() && productId != null) {
            result = service.searchByProductIdAndName(productId, search);
        } else if (search != null && !search.isBlank()) {
            result = service.search(search);
        } else if (productId != null && standardReference != null && !standardReference.isBlank()) {
            result = service.listByProductIdAndStandard(productId, standardReference);
        } else if (productId != null) {
            result = service.listByProductId(productId);
        } else if (standardReference != null && !standardReference.isBlank()) {
            result = service.listByStandard(standardReference);
        } else {
            result = service.listAll();
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_READ + "')")
    public ResponseEntity<ApiResponse<TestParameterResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    // --- CREATE ---

    @PostMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_CREATE + "')")
    public ResponseEntity<ApiResponse<TestParameterResponse>> create(
            @Valid @RequestBody TestParameterRequest request) {
        TestParameterResponse response = service.create(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // --- UPDATE ---

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_UPDATE + "')")
    public ResponseEntity<ApiResponse<TestParameterResponse>> update(
            @PathVariable Long id,
            @RequestBody TestParameterUpdateRequest request) {
        TestParameterResponse response = service.update(id, request, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- DELETE ---

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_DELETE + "')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Test parameter deleted successfully"));
    }
}
