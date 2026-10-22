package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.dto.request.TestParameterRequest;
import com.medplus.qc_lims_backend.dto.request.TestParameterUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TestParameterResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.DuplicateResourceException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import com.medplus.qc_lims_backend.service.TestParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Test Parameter Master CRUD operations.
 *
 * Handles business rules, validation, and entity-to-DTO conversion.
 * Class-level @Transactional(readOnly=true) for read methods;
 * individual mutating methods override with @Transactional.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestParameterServiceImpl implements TestParameterService {

    private final TestParameterMasterRepository repository;

    // --- READ operations (readOnly by default) ---

    @Override
    public List<TestParameterResponse> listAll() {
        return repository.findByStatus(RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TestParameterResponse> listByProductId(Integer productId) {
        return repository.findByProductIdAndStatus(productId, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TestParameterResponse> listByProductIdAndStandard(Integer productId, String standardReference) {
        return listByProductId(productId).stream()
                .filter(r -> standardReference.equals(r.standardReference()))
                .toList();
    }

    @Override
    public List<TestParameterResponse> listByStandard(String standardReference) {
        return listAll().stream()
                .filter(r -> standardReference.equals(r.standardReference()))
                .toList();
    }

    @Override
    public TestParameterResponse getById(Long id) {
        TestParameterMaster entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestParameter", id));
        return toResponse(entity);
    }

    @Override
    public List<TestParameterResponse> search(String parameterName) {
        if (parameterName == null || parameterName.isBlank()) {
            return listAll();
        }
        return repository.findByParameterNameContainingIgnoreCaseAndStatus(parameterName, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TestParameterResponse> searchByProductIdAndName(Integer productId, String parameterName) {
        if (parameterName == null || parameterName.isBlank()) {
            return listByProductId(productId);
        }
        return repository.findByProductIdAndParameterNameContainingIgnoreCaseAndStatus(
                productId, parameterName, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    // --- CREATE (mutating) ---

    @Override
    @Transactional
    public TestParameterResponse create(TestParameterRequest request, Long createdBy) {
        validateBusinessRules(request);

        // Check duplicate (product_id, parameter_name)
        if (repository.existsByProductIdAndParameterNameAndStatus(
                request.getProductId(), request.getParameterName(), RecordStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "Test parameter with product ID " + request.getProductId()
                            + " and name '" + request.getParameterName() + "' already exists.");
        }

        TestParameterMaster entity = new TestParameterMaster();
        populateEntity(entity, request);
        entity.setCreatedBy(createdBy);
        entity.setStatus(RecordStatus.ACTIVE);

        try {
            entity = repository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "A test parameter with this product ID and name already exists.");
        }
        return toResponse(entity);
    }

    // --- UPDATE (mutating) — Partial update with optional fields ---

    @Override
    @Transactional
    public TestParameterResponse update(Long id, TestParameterUpdateRequest request, Long updatedBy) {
        TestParameterMaster entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestParameter", id));

        // Only validate business rules if limits are actually being changed
        if (request.getMinLimit() != null && request.getMaxLimit() != null) {
            if (request.getMinLimit().compareTo(request.getMaxLimit()) >= 0) {
                throw new BusinessRuleViolationException(
                        "Min limit (" + request.getMinLimit()
                        + ") must be less than max limit (" + request.getMaxLimit() + ").");
            }
        }

        // Check duplicate only if both productId and parameterName are being set
        if (request.getProductId() != null && request.getParameterName() != null) {
            if (repository.existsByProductIdAndParameterNameAndIdNotAndStatus(
                    request.getProductId(), request.getParameterName(), id, RecordStatus.ACTIVE)) {
                throw new DuplicateResourceException(
                        "Test parameter with product ID " + request.getProductId()
                                + " and name '" + request.getParameterName() + "' already exists.");
            }
        }

        // Apply partial update — only set non-null fields
        if (request.getProductId() != null) entity.setProductId(request.getProductId());
        if (request.getParameterName() != null) entity.setParameterName(request.getParameterName());
        if (request.getStandardReference() != null) entity.setStandardReference(request.getStandardReference());
        if (request.getStpDocumentNumber() != null) entity.setStpDocumentNumber(request.getStpDocumentNumber());
        if (request.getStpVersionNumber() != null) entity.setStpVersionNumber(request.getStpVersionNumber());
        if (request.getMinLimit() != null) entity.setMinLimit(request.getMinLimit());
        if (request.getMaxLimit() != null) entity.setMaxLimit(request.getMaxLimit());
        if (request.getTextSpecification() != null) entity.setTextSpecification(request.getTextSpecification());
        if (request.getIsCritical() != null) entity.setIsCritical(request.getIsCritical());
        entity.setUpdatedBy(updatedBy);

        try {
            entity = repository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "A test parameter with this product ID and name already exists.");
        }
        return toResponse(entity);
    }

    // --- DELETE (mutating) — Soft delete per RC-2 ---

    @Override
    @Transactional
    public void delete(Long id) {
        TestParameterMaster entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestParameter", id));

        // Soft delete: set status to INACTIVE instead of hard delete
        // This avoids FK violations with trf_test_parameters (no ON DELETE CASCADE)
        entity.setStatus(RecordStatus.INACTIVE);
        repository.save(entity);
    }

    // --- Business rule validation ---

    private void validateBusinessRules(TestParameterRequest request) {
        // V-05: minLimit < maxLimit if both provided
        if (request.getMinLimit() != null && request.getMaxLimit() != null) {
            if (request.getMinLimit().compareTo(request.getMaxLimit()) >= 0) {
                throw new BusinessRuleViolationException(
                        "Min limit (" + request.getMinLimit()
                        + ") must be less than max limit (" + request.getMaxLimit() + ").");
            }
        }

        // V-06: At least one of (minLimit, maxLimit) or textSpecification must be provided
        if (request.getMinLimit() == null && request.getMaxLimit() == null
                && (request.getTextSpecification() == null || request.getTextSpecification().isBlank())) {
            throw new BusinessRuleViolationException(
                    "At least one of min limit, max limit, or text specification must be provided.");
        }
    }

    // --- Entity population helper ---

    private void populateEntity(TestParameterMaster entity, TestParameterRequest request) {
        entity.setProductId(request.getProductId());
        entity.setParameterName(request.getParameterName());
        entity.setStandardReference(request.getStandardReference());
        entity.setStpDocumentNumber(request.getStpDocumentNumber());
        entity.setStpVersionNumber(request.getStpVersionNumber());
        entity.setMinLimit(request.getMinLimit());
        entity.setMaxLimit(request.getMaxLimit());
        entity.setTextSpecification(request.getTextSpecification());
        entity.setIsCritical(request.getIsCritical() != null ? request.getIsCritical() : true);
    }

    // --- DTO conversion ---

    private TestParameterResponse toResponse(TestParameterMaster entity) {
        return TestParameterResponse.fromEntity(entity);
    }
}
