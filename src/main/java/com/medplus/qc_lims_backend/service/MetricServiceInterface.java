package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.criteria.MetricSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MetricServiceInterface {

    PagedResponse<Metric> getPagedMetrics(MetricSearchCriteria criteria);

    List<Metric> findList(MetricSearchCriteria criteria, int maxResults);

    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);

    byte[] downloadTemplate();
}
