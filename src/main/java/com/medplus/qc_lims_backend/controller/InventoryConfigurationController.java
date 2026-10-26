package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.InventoryConfigurationServiceInterface;
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
@RequestMapping("/api/masters/inventory-config")
@PreAuthorize("hasAuthority('" + Rights.ROLE_INVENTORY_CONFIG_READ + "')")
public class InventoryConfigurationController {

    private final InventoryConfigurationServiceInterface inventoryConfigurationService;

    public InventoryConfigurationController(InventoryConfigurationServiceInterface inventoryConfigurationService) {
        this.inventoryConfigurationService = inventoryConfigurationService;
    }

    @GetMapping
    public Page<InventoryConfiguration> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable) {
        return inventoryConfigurationService.findAll(search, includeInactive, pageable);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_INVENTORY_CONFIG_UPLOAD + "')")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = inventoryConfigurationService.uploadExcel(file, employeeId);

        if (result.success()) {
            return ResponseEntity.ok(result);
        }

        byte[] errorFile = result.errorFileBytes();
        if (errorFile != null) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"inventory_config_errors.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(errorFile);
        }

        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_INVENTORY_CONFIG_UPLOAD + "')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = inventoryConfigurationService.downloadTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"inventory_config_template.xlsx\"");
        response.getOutputStream().write(data);
    }
}
