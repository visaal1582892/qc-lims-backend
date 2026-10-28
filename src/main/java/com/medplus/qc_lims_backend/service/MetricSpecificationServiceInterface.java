package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.criteria.MetricSpecificationSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MetricSpecificationServiceInterface {

    PagedResponse<MetricSpecification> getPagedMetricSpecifications(MetricSpecificationSearchCriteria criteria);

    List<MetricSpecification> findList(MetricSpecificationSearchCriteria criteria, int maxResults);

    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);

    byte[] downloadTemplate();
}
