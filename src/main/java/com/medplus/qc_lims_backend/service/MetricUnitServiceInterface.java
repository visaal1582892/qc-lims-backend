package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Rules.md §3.1 — Service interface.
 */
public interface MetricUnitServiceInterface {

    Page<MetricUnit> findAll(String search, boolean includeInactive, Pageable pageable);
    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);
    byte[] downloadTemplate();
}
