package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.domain.criteria.TestParameterSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestParameterServiceInterface {

    PagedResponse<TestParameter> getPagedTestParameters(TestParameterSearchCriteria criteria);
    List<TestParameter> findList(TestParameterSearchCriteria criteria, int maxResults);
    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);
    byte[] downloadTemplate();
}
