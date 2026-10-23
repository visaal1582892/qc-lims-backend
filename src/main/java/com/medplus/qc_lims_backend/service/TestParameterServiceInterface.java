package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface TestParameterServiceInterface {

    Page<TestParameter> findAll(boolean includeInactive, Pageable pageable);
    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);
    byte[] downloadTemplate();
}
