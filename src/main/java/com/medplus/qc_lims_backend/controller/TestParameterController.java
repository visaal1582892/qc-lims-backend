package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.TestParameterServiceInterface;
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
@RequestMapping("/api/masters/test-parameters")
@PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_READ + "')")
public class TestParameterController {

    private final TestParameterServiceInterface testParameterService;

    public TestParameterController(TestParameterServiceInterface testParameterService) {
        this.testParameterService = testParameterService;
    }

    @GetMapping
    public Page<TestParameter> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable) {
        return testParameterService.findAll(search, includeInactive, pageable);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_TEST_PARAMETER_UPLOAD + "')")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = testParameterService.uploadExcel(file, employeeId);

        if (result.success()) {
            return ResponseEntity.ok(result);
        }

        byte[] errorFile = result.errorFileBytes();
        if (errorFile != null) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"test_parameter_errors.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(errorFile);
        }

        return ResponseEntity.badRequest().body(result);
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
