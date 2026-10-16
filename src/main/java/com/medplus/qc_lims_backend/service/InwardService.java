package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.CreateInwardRequest;
import com.medplus.qc_lims_backend.dto.request.UpdateInwardRequest;
import com.medplus.qc_lims_backend.dto.response.InwardResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service layer for Inward Registration CRUD operations.
 *
 * AR number generation via ArSequenceGenerator (atomic zero-gap, RC-4).
 * Soft delete via status=INACTIVE (RC-7).
 * Follows TestParameterService pattern exactly.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InwardService {

    private final SampleInwardRepository repository;
    private final ArSequenceGenerator arSequenceGenerator;

    // --- READ operations (readOnly by default) ---

    public List<InwardResponse> listAll() {
        return repository.findByStatus(RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public InwardResponse getById(Long id) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));
        return toResponse(entity);
    }

    public InwardResponse getByArNumber(String arNumber) {
        SampleInwardRegister entity = repository.findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration with AR number", arNumber));
        return toResponse(entity);
    }

    public List<InwardResponse> listByProductId(Integer productId) {
        return repository.findByProductIdAndStatus(productId, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<InwardResponse> searchByBatchNo(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) {
            return listAll();
        }
        return repository.findByBatchNoContainingIgnoreCaseAndStatus(batchNo, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    // --- CREATE (mutating) ---

    @Transactional
    public InwardResponse create(CreateInwardRequest request, Long createdBy) {
        // Business rule: V-08 totalReceivedUnits >= receivedQuantityPacks
        if (request.getTotalReceivedUnits() < request.getReceivedQuantityPacks()) {
            throw new BusinessRuleViolationException(
                "Total received units (" + request.getTotalReceivedUnits() +
                ") cannot be less than received quantity packs (" + request.getReceivedQuantityPacks() + ").");
        }

        // Business rule: V-07 validate storageLocation (handled by @Pattern in DTO)

        SampleInwardRegister entity = new SampleInwardRegister();
        populateEntity(entity, request);
        entity.setCreatedBy(createdBy);
        entity.setStatus(RecordStatus.ACTIVE);
        entity.setReceiptDate(LocalDate.now());

        boolean isSubcontracted = Boolean.TRUE.equals(request.getIsSubcontracted());

        // V-01/V-02: Generate AR number for non-subcontracted; NULL for subcontracted
        if (!isSubcontracted) {
            entity.setArNumber(arSequenceGenerator.generateArNumber());
        } else {
            entity.setArNumber(null);
        }

        entity = repository.save(entity);
        return toResponse(entity);
    }

    // --- UPDATE (mutating) ---

    @Transactional
    public InwardResponse update(Long id, UpdateInwardRequest request, Long updatedBy) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        // Business rule: no AR number change after creation
        // (AR number is immutable once assigned)

        // Validate total >= packs if both provided
        if (request.getTotalReceivedUnits() != null && request.getReceivedQuantityPacks() != null) {
            if (request.getTotalReceivedUnits() < request.getReceivedQuantityPacks()) {
                throw new BusinessRuleViolationException(
                    "Total received units cannot be less than received quantity packs.");
            }
        }

        // Apply partial update — only non-null fields
        if (request.getProductId() != null) entity.setProductId(request.getProductId());
        if (request.getBatchNo() != null) entity.setBatchNo(request.getBatchNo());
        if (request.getReceivedQuantityPacks() != null) entity.setReceivedQuantityPacks(request.getReceivedQuantityPacks());
        if (request.getTotalReceivedUnits() != null) entity.setTotalReceivedUnits(request.getTotalReceivedUnits());
        if (request.getTrfId() != null) entity.setTrfId(request.getTrfId());
        if (request.getStorageLocation() != null) entity.setStorageLocation(request.getStorageLocation());
        if (request.getIsSubcontracted() != null) entity.setIsSubcontracted(request.getIsSubcontracted());
        if (request.getSubcontractRemarks() != null) entity.setSubcontractRemarks(request.getSubcontractRemarks());

        entity.setUpdatedBy(updatedBy);

        entity = repository.save(entity);
        return toResponse(entity);
    }

    // --- DELETE (mutating) — Soft delete per RC-7 ---

    @Transactional
    public void delete(Long id) {
        SampleInwardRegister entity = repository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("InwardRegistration", id));

        entity.setStatus(RecordStatus.INACTIVE);
        repository.save(entity);
    }

    // --- Entity population helper ---

    private void populateEntity(SampleInwardRegister entity, CreateInwardRequest request) {
        entity.setProductId(request.getProductId());
        entity.setBatchNo(request.getBatchNo());
        entity.setReceivedQuantityPacks(request.getReceivedQuantityPacks());
        entity.setTotalReceivedUnits(request.getTotalReceivedUnits());
        entity.setTrfId(request.getTrfId());
        entity.setStorageLocation(request.getStorageLocation());
        entity.setIsSubcontracted(request.getIsSubcontracted() != null ? request.getIsSubcontracted() : false);
        entity.setSubcontractRemarks(request.getSubcontractRemarks());
    }

    // --- DTO conversion ---

    private InwardResponse toResponse(SampleInwardRegister entity) {
        return InwardResponse.fromEntity(entity);
    }
}
