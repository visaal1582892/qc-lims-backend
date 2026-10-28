package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TestParameterServiceInterface;
import com.medplus.qc_lims_backend.domain.criteria.TestParameterSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/masters/test-parameters")
@PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_READ + "')")
public class TestParameterController {

    private final TestParameterServiceInterface testParameterService;

    public TestParameterController(TestParameterServiceInterface testParameterService) {
        this.testParameterService = testParameterService;
    }

    @GetMapping
    public PagedResponse<TestParameter> getAll(
            @ModelAttribute TestParameterSearchCriteria criteria,
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
        return testParameterService.getPagedTestParameters(criteria);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_UPLOAD + "')")
    public ResponseEntity<BulkUploadResponse> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = testParameterService.uploadExcel(file, employeeId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_UPLOAD + "')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = testParameterService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"test_parameter_template.xlsx\"");
        response.getOutputStream().write(data);
    }
}
