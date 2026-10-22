package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.TestParameterRequest;
import com.medplus.qc_lims_backend.dto.request.TestParameterUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TestParameterResponse;

import java.util.List;

/**
 * Service layer interface for Test Parameter Master CRUD operations.
 *
 * Handles business rules, validation, and entity-to-DTO conversion.
 */
public interface TestParameterService {

    List<TestParameterResponse> listAll();

    List<TestParameterResponse> listByProductId(Integer productId);

    List<TestParameterResponse> listByProductIdAndStandard(Integer productId, String standardReference);

    List<TestParameterResponse> listByStandard(String standardReference);

    TestParameterResponse getById(Long id);

    List<TestParameterResponse> search(String parameterName);

    List<TestParameterResponse> searchByProductIdAndName(Integer productId, String parameterName);

    TestParameterResponse create(TestParameterRequest request, Long createdBy);

    TestParameterResponse update(Long id, TestParameterUpdateRequest request, Long updatedBy);

    void delete(Long id);
}
