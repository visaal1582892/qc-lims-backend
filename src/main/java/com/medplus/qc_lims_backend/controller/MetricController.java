package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.criteria.MetricSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.MetricServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping({"/api/masters/metrics", "/api/masters/metric-units"})
@PreAuthorize("hasAnyAuthority('" + Rights.ROLE_METRIC_READ + "', 'ROLE_METRIC_UNIT_READ')")
public class MetricController {

    private final MetricServiceInterface metricService;

    public MetricController(MetricServiceInterface metricService) {
        this.metricService = metricService;
    }

    @GetMapping
    public PagedResponse<Metric> getAll(
            @ModelAttribute MetricSearchCriteria criteria,
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
        return metricService.getPagedMetrics(criteria);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('" + Rights.ROLE_METRIC_UPLOAD + "', 'ROLE_METRIC_UNIT_UPLOAD')")
    public ResponseEntity<BulkUploadResponse> uploadExcel(@RequestParam("file") MultipartFile file,
                                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = metricService.uploadExcel(file, employeeId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyAuthority('" + Rights.ROLE_METRIC_UPLOAD + "', 'ROLE_METRIC_UNIT_UPLOAD')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = metricService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"metrics_template.xlsx\"");
        response.getOutputStream().write(data);
    }
}
