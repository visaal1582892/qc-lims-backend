package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.AuthorizeRetestRequest;
import com.medplus.qc_lims_backend.dto.request.CompleteRetestRequest;
import com.medplus.qc_lims_backend.dto.request.OosInvestigationRequest;
import com.medplus.qc_lims_backend.dto.request.RaiseOosIncidentRequest;
import com.medplus.qc_lims_backend.dto.request.RejectBatchRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.OosIncidentResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.OosIncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for OOS Incident Management (SRS Module 8).
 *
 * Endpoints:
 * - OIS-01: POST /{id}/authorize-retest   — QC Head authorizes retest
 * - OIS-02: POST /{id}/complete-retest    — Complete retest with revised result
 * - OIS-03: POST /{id}/reject-batch       — QC Head rejects batch
 * - OIS-04: GET  /                        — List incidents with filters
 * - OIS-05: GET  /{id}                   — Get incident detail
 * - OIS-06: GET  /by-ar/{arNumber}       — List incidents by AR number
 */
@RestController
@RequestMapping("/api/v1/oos-incidents")
@RequiredArgsConstructor
public class OosIncidentController {

    private final OosIncidentService oosIncidentService;

    // OIS-01: Authorize Retest (QC Head)
    @PostMapping("/{id}/authorize-retest")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_RAISE + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> authorizeRetest(
            @PathVariable Long id,
            @Valid @RequestBody AuthorizeRetestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.authorizeRetest(id, request)));
    }

    // OIS-02: Complete Retest
    @PostMapping("/{id}/complete-retest")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_RETEST + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> completeRetest(
            @PathVariable Long id,
            @Valid @RequestBody CompleteRetestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.completeRetest(id, request)));
    }

    // OIS-03: Reject Batch
    @PostMapping("/{id}/reject-batch")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_BATCH_REJECT + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> rejectBatch(
            @PathVariable Long id,
            @Valid @RequestBody RejectBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.rejectBatch(id, request)));
    }

    // OIS-04: List OOS Incidents (with filters)
    @GetMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_VIEW + "')")
    public ResponseEntity<ApiResponse<Page<OosIncidentResponse>>> listIncidents(
            @RequestParam(required = false) String arNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String disposition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.listIncidents(arNumber, status, disposition, pageRequest)));
    }

    // OIS-05: Get Incident Detail
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_VIEW + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> getIncident(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(oosIncidentService.getIncident(id)));
    }

    // OIS-06: List Incidents by AR Number
    @GetMapping("/by-ar/{arNumber}")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_VIEW + "')")
    public ResponseEntity<ApiResponse<List<OosIncidentResponse>>> getIncidentsByAr(
            @PathVariable String arNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.getIncidentsByAr(arNumber)));
    }

    // OIS-07: Manually raise an OOS incident
    @PostMapping("/raise")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_RAISE + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> raiseOosIncident(
            @Valid @RequestBody RaiseOosIncidentRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok(oosIncidentService.raiseOosIncident(request)));
    }

    // OIS-08: Submit investigation findings
    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_OOS_RAISE + "')")
    public ResponseEntity<ApiResponse<OosIncidentResponse>> investigate(
            @PathVariable Long id,
            @Valid @RequestBody OosInvestigationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                oosIncidentService.investigate(id, request)));
    }
}
