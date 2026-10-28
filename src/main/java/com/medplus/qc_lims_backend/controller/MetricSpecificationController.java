package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.criteria.MetricSpecificationSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.MetricSpecificationServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/masters/metric-specifications")
@PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_SPECIFICATION_READ + "')")
public class MetricSpecificationController {

    private final MetricSpecificationServiceInterface metricSpecificationService;

    public MetricSpecificationController(MetricSpecificationServiceInterface metricSpecificationService) {
        this.metricSpecificationService = metricSpecificationService;
    }

    @GetMapping
    public PagedResponse<MetricSpecification> getAll(
            @ModelAttribute MetricSpecificationSearchCriteria criteria,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            criteria.setSortField(parts[0].trim());
            if (parts.length > 1) {
                criteria.setSortDirection(parts[1].trim());
            }
        }
        if (StringUtils.hasText(search) && !StringUtils.hasText(criteria.getSearchKey())) {
            criteria.setSearchKey(search);
        }
        return metricSpecificationService.getPagedMetricSpecifications(criteria);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_SPECIFICATION_UPLOAD + "')")
    public ResponseEntity<BulkUploadResponse> uploadExcel(@RequestParam("file") MultipartFile file,
                                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = metricSpecificationService.uploadExcel(file, employeeId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_METRIC_SPECIFICATION_UPLOAD + "')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = metricSpecificationService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"metric_specifications_template.xlsx\"");
        response.getOutputStream().write(data);
    }
}
