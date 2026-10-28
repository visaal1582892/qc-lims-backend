package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.InventoryConfigurationServiceInterface;
import com.medplus.qc_lims_backend.domain.criteria.InventoryConfigSearchCriteria;
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
@RequestMapping("/api/masters/inventory-config")
@PreAuthorize("hasAuthority('" + Rights.ROLE_INVENTORY_CONFIG_READ + "')")
public class InventoryConfigurationController {

    private final InventoryConfigurationServiceInterface inventoryConfigurationService;

    public InventoryConfigurationController(InventoryConfigurationServiceInterface inventoryConfigurationService) {
        this.inventoryConfigurationService = inventoryConfigurationService;
    }

    @GetMapping
    public PagedResponse<InventoryConfiguration> getAll(
            @ModelAttribute InventoryConfigSearchCriteria criteria,
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
        return inventoryConfigurationService.getPagedInventoryConfig(criteria);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_INVENTORY_CONFIG_UPLOAD + "')")
    public ResponseEntity<BulkUploadResponse> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestHeader("X-Employee-ID") String employeeId) {
        BulkUploadResponse result = inventoryConfigurationService.uploadExcel(file, employeeId);
        return ResponseEntity.ok(result);
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
