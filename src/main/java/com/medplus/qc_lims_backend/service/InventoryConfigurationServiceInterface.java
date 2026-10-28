package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.domain.criteria.InventoryConfigSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InventoryConfigurationServiceInterface {

    PagedResponse<InventoryConfiguration> getPagedInventoryConfig(InventoryConfigSearchCriteria criteria);

    List<InventoryConfiguration> findList(InventoryConfigSearchCriteria criteria, int maxResults);

    BulkUploadResponse uploadExcel(MultipartFile file, String employeeId);

    byte[] downloadTemplate();
}
