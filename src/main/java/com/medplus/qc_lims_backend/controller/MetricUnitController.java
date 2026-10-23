package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.MetricUnitServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.medplus.qc_lims_backend.security.CustomUserDetails;

/**
 * Metric Unit Master — CRUD via Excel bulk upload.
 * goal.md §3 — Upload Strategy, §4 — Frontend Architecture.
 */
@RestController
@RequestMapping("/api/masters/metric-units")
public class MetricUnitController {

    private final MetricUnitServiceInterface service;

    public MetricUnitController(MetricUnitServiceInterface service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_READ + "')")
    public ResponseEntity<Page<MetricUnit>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(includeInactive, pageable));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_UPLOAD + "')")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BulkUploadResponse response = service.uploadExcel(file, userDetails.getUser().getEmployeeId());

        // If validation failed and error file is available, return it as a downloadable blob
        if (!response.success() && response.errorFileBytes() != null) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metric_unit_errors.xlsx\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_UPLOAD + "')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = service.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"metric_unit_template.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }
}
