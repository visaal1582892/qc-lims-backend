package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.domain.criteria.AnalystQualificationSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnalystQualificationServiceInterface {

    PagedResponse<AnalystQualification> getPagedQualifications(AnalystQualificationSearchCriteria criteria);

    List<AnalystQualification> findList(AnalystQualificationSearchCriteria criteria, int maxResults);

    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);

    byte[] downloadTemplate();
}
