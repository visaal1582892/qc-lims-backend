package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.CustomUserDetails;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.AnalystQualificationServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Analyst Qualification Master — CRUD via Excel bulk upload.
 */
@RestController
@RequestMapping("/api/masters/analyst-qualifications")
public class AnalystQualificationController {

    private final AnalystQualificationServiceInterface service;

    public AnalystQualificationController(AnalystQualificationServiceInterface service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Rights.ROLE_QUALIFICATION_READ + "')")
    public ResponseEntity<Page<AnalystQualification>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(includeInactive, pageable));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + Rights.ROLE_QUALIFICATION_UPLOAD + "')")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BulkUploadResponse response = service.uploadExcel(file, userDetails.getUser().getEmployeeId());

        if (!response.success() && response.errorFileBytes() != null) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analyst_qualification_errors.xlsx\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_QUALIFICATION_UPLOAD + "')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = service.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"analyst_qualification_template.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }
}
