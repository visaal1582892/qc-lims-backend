package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.MetricUnitServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/masters/metric-units")
@PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_READ + "')")
public class MetricUnitController {

    private final MetricUnitServiceInterface metricUnitService;

    public MetricUnitController(MetricUnitServiceInterface metricUnitService) {
        this.metricUnitService = metricUnitService;
    }

    @GetMapping
    public Page<MetricUnit> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable) {
        return metricUnitService.findAll(search, includeInactive, pageable);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_UPLOAD + "')")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = metricUnitService.uploadExcel(file, employeeId);

        if (result.success()) {
            return ResponseEntity.ok(result);
        }

        // On validation failure, return the error file bytes directly (not the DTO)
        byte[] errorFile = result.errorFileBytes();
        if (errorFile != null) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"metric_unit_errors.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(errorFile);
        }

        // No error file — return JSON error
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_UNIT_UPLOAD + "')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = metricUnitService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"metric_unit_template.xlsx\"");
        response.getOutputStream().write(data);
    }
}
