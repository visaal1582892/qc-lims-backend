package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TrialTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Trial Test / For Information controller (SRS Module 9).
 *
 * Endpoints:
 * - TT-01: POST /{arNumber}/mark-for-information — Mark AR as For_Information
 * - TT-02: POST /{arNumber}/generate-fresh-ar    — Generate fresh AR from For_Information AR
 * - TT-03: GET  /{arNumber}                      — Get For_Information AR detail
 */
@RestController
@RequestMapping("/api/v1/trial-tests")
@RequiredArgsConstructor
public class TrialTestController {

    private final TrialTestService trialTestService;

    /**
     * TT-01: Mark AR as For_Information (Trial Test).
     * Transitions AR from COMPLETED/ALL_TESTS_COMPLETE to FOR_INFORMATION status.
     */
    @PostMapping("/{arNumber}/mark-for-information")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TRIAL_TEST_CLOSE + "')")
    public ResponseEntity<ApiResponse<InwardResponse>> markForInformation(
            @PathVariable String arNumber,
            @RequestParam(required = false, defaultValue = "Trial test required") String reason) {
        var inward = trialTestService.markForInformation(arNumber, reason);
        return ResponseEntity.ok(ApiResponse.ok(InwardResponse.fromEntity(inward)));
    }

    /**
     * TT-02: Generate fresh AR from For_Information AR.
     * Creates a new AR linked back to the For_Information AR via linkedForInfoArNumber.
     */
    @PostMapping("/{arNumber}/generate-fresh-ar")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_AR_GENERATE_FRESH + "')")
    public ResponseEntity<ApiResponse<InwardResponse>> generateFreshAr(
            @PathVariable String arNumber) {
        var inward = trialTestService.generateFreshAr(arNumber);
        return ResponseEntity.ok(ApiResponse.ok(InwardResponse.fromEntity(inward)));
    }

    /**
     * TT-03: Get For_Information AR detail.
     */
    @GetMapping("/{arNumber}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_VIEW + "')")
    public ResponseEntity<ApiResponse<InwardResponse>> getForInfoAr(
            @PathVariable String arNumber) {
        var inward = trialTestService.getForInfoAr(arNumber);
        return ResponseEntity.ok(ApiResponse.ok(InwardResponse.fromEntity(inward)));
    }
}
