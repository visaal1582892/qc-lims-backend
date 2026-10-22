package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.domain.TrfTestParameter;
import com.medplus.qc_lims_backend.dto.request.TrfCreateRequest;
import com.medplus.qc_lims_backend.dto.request.TrfUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TrfResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import com.medplus.qc_lims_backend.repository.TrfTestParameterRepository;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import com.medplus.qc_lims_backend.service.TrfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation for e-TRF Management CRUD and lifecycle operations.
 *
 * Handles lifecycle: Pending_Feasibility -> Accepted/Rejected (SRS 3.2).
 * Follows TestParameterService pattern exactly.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrfServiceImpl implements TrfService {

    private final TrfRepository trfRepository;
    private final TrfTestParameterRepository trfTestParameterRepository;
    private final TestParameterMasterRepository testParameterRepository;

    // --- READ operations ---

    @Override
    public List<TrfResponse> listAll() {
        return trfRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TrfResponse getById(Long id) {
        TrfMaster entity = trfRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", id));
        return toResponse(entity);
    }

    @Override
    public List<TrfResponse> listByStatus(String trfStatus) {
        return trfRepository.findByTrfStatusAndStatus(trfStatus, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TrfResponse> listByProductId(Integer productId) {
        return trfRepository.findByProductIdAndStatus(productId, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TrfResponse> searchByGrnNumber(String grnNumber) {
        if (grnNumber == null || grnNumber.isBlank()) {
            return listAll();
        }
        return trfRepository.findByGrnNumberContainingIgnoreCaseAndStatus(grnNumber, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    // --- CREATE (mutating) ---

    @Override
    @Transactional
    public TrfResponse create(TrfCreateRequest request, Long createdBy) {
        // Validate business rules
        validateBusinessRules(request);

        TrfMaster entity = new TrfMaster();
        populateEntity(entity, request);
        entity.setCreatedBy(createdBy);
        // B-1 fix: submittedByUserId from service-layer context, not client input
        entity.setSubmittedByUserId(createdBy != null ? createdBy.intValue() : 1);
        entity.setStatus(RecordStatus.ACTIVE);
        entity.setTrfStatus("Pending_Feasibility");
        entity.setSubmittedAt(LocalDateTime.now());

        entity = trfRepository.save(entity);

        // Assign test parameters via junction table
        List<Integer> paramIds = request.getTestParameterIds();
        if (paramIds != null && !paramIds.isEmpty()) {
            for (Integer paramId : paramIds) {
                // Verify parameter exists
                TestParameterMaster param = testParameterRepository.findByIdAndStatus(
                        paramId.longValue(), RecordStatus.ACTIVE)
                    .orElseThrow(() -> new EntityNotFoundException("TestParameter", paramId));

                TrfTestParameter junction = new TrfTestParameter();
                junction.setTrfId(entity.getId().intValue());
                junction.setParameterId(paramId);
                junction.setIsActiveForTesting(true);
                junction.setStatus(RecordStatus.ACTIVE);
                trfTestParameterRepository.save(junction);
            }
        }

        return toResponse(entity);
    }

    // --- UPDATE (mutating) ---

    @Override
    @Transactional
    public TrfResponse update(Long id, TrfUpdateRequest request, Long updatedBy) {
        TrfMaster entity = trfRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", id));

        // If trfStatus is being changed, validate lifecycle transitions
        if (request.getTrfStatus() != null && !request.getTrfStatus().isBlank()) {
            validateStatusTransition(entity.getTrfStatus(), request.getTrfStatus());
            entity.setTrfStatus(request.getTrfStatus());
        }

        // Apply partial update
        if (request.getCompanyName() != null) entity.setCompanyName(request.getCompanyName());
        if (request.getCompanyAddress() != null) entity.setCompanyAddress(request.getCompanyAddress());
        if (request.getManufacturingLicNo() != null) entity.setManufacturingLicNo(request.getManufacturingLicNo());
        if (request.getContactPersonName() != null) entity.setContactPersonName(request.getContactPersonName());
        if (request.getPhoneMobile() != null) entity.setPhoneMobile(request.getPhoneMobile());
        if (request.getEmailId() != null) entity.setEmailId(request.getEmailId());
        if (request.getProductId() != null) entity.setProductId(request.getProductId());
        if (request.getBatchNumbers() != null) entity.setBatchNumbers(request.getBatchNumbers());
        if (request.getCustomerArNo() != null) entity.setCustomerArNo(request.getCustomerArNo());
        if (request.getMaterialType() != null) entity.setMaterialType(request.getMaterialType());
        if (request.getOtherMaterialTypeDesc() != null) entity.setOtherMaterialTypeDesc(request.getOtherMaterialTypeDesc());
        if (request.getMfgDate() != null) entity.setMfgDate(request.getMfgDate());
        if (request.getExpDate() != null) entity.setExpDate(request.getExpDate());
        if (request.getRetestDate() != null) entity.setRetestDate(request.getRetestDate());
        if (request.getBatchSize() != null) entity.setBatchSize(request.getBatchSize());
        if (request.getSampleQty() != null) entity.setSampleQty(request.getSampleQty());
        if (request.getPackingCondition() != null) entity.setPackingCondition(request.getPackingCondition());
        if (request.getStorageCondition() != null) entity.setStorageCondition(request.getStorageCondition());
        if (request.getOtherStorageDesc() != null) entity.setOtherStorageDesc(request.getOtherStorageDesc());
        if (request.getIsHygroscopic() != null) entity.setIsHygroscopic(request.getIsHygroscopic());
        if (request.getIsLightSensitive() != null) entity.setIsLightSensitive(request.getIsLightSensitive());
        if (request.getIsNonHazardous() != null) entity.setIsNonHazardous(request.getIsNonHazardous());
        if (request.getIsHazardous() != null) entity.setIsHazardous(request.getIsHazardous());
        if (request.getIsMsdsAttached() != null) entity.setIsMsdsAttached(request.getIsMsdsAttached());
        if (request.getOtherSampleTypeDesc() != null) entity.setOtherSampleTypeDesc(request.getOtherSampleTypeDesc());
        if (request.getSubmissionType() != null) entity.setSubmissionType(request.getSubmissionType());
        if (request.getSubmissionTypeDesc() != null) entity.setSubmissionTypeDesc(request.getSubmissionTypeDesc());
        if (request.getAnalysisType() != null) entity.setAnalysisType(request.getAnalysisType());
        if (request.getRegulatoryFormType() != null) entity.setRegulatoryFormType(request.getRegulatoryFormType());
        if (request.getPharmacopoeiaReference() != null) entity.setPharmacopoeiaReference(request.getPharmacopoeiaReference());
        if (request.getStpDocumentNo() != null) entity.setStpDocumentNo(request.getStpDocumentNo());
        if (request.getStpVersionNo() != null) entity.setStpVersionNo(request.getStpVersionNo());
        if (request.getGrnNumber() != null) entity.setGrnNumber(request.getGrnNumber());
        if (request.getManufacturerId() != null) entity.setManufacturerId(request.getManufacturerId());

        entity.setUpdatedBy(updatedBy);

        // If test parameters are being updated, replace junction records
        // M-3: Guard against empty list — prevents deleting all parameters without adding any
        if (request.getTestParameterIds() != null && !request.getTestParameterIds().isEmpty()) {
            trfTestParameterRepository.softDeleteByTrfIdAndStatus(entity.getId().intValue(), RecordStatus.ACTIVE);
            for (Integer paramId : request.getTestParameterIds()) {
                TrfTestParameter junction = new TrfTestParameter();
                junction.setTrfId(entity.getId().intValue());
                junction.setParameterId(paramId);
                junction.setIsActiveForTesting(true);
                junction.setStatus(RecordStatus.ACTIVE);
                trfTestParameterRepository.save(junction);
            }
        }

        entity = trfRepository.save(entity);
        return toResponse(entity);
    }

    // --- LIFECYCLE: Feasibility decision ---

    @Override
    @Transactional
    public TrfResponse accept(Long id, Long updatedBy) {
        TrfMaster entity = trfRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", id));
        validateStatusTransition(entity.getTrfStatus(), "Accepted");
        entity.setTrfStatus("Accepted");
        entity.setUpdatedBy(updatedBy);
        entity = trfRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public TrfResponse reject(Long id, Long updatedBy) {
        TrfMaster entity = trfRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", id));
        validateStatusTransition(entity.getTrfStatus(), "Rejected");
        entity.setTrfStatus("Rejected");
        entity.setUpdatedBy(updatedBy);
        entity = trfRepository.save(entity);
        return toResponse(entity);
    }

    // --- SOFT DELETE ---

    @Override
    @Transactional
    public void delete(Long id) {
        TrfMaster entity = trfRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", id));
        entity.setStatus(RecordStatus.INACTIVE);
        trfRepository.save(entity);
    }

    // --- ASSIGN TEST PARAMETERS (QC Head customization) ---

    @Override
    @Transactional
    public void assignTestParameters(Long trfId, List<Integer> parameterIds, Long updatedBy) {
        TrfMaster entity = trfRepository.findByIdAndStatus(trfId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("e-TRF", trfId));

        // M-2: Guard against empty parameter list — prevents accidental deletion of all parameters
        if (parameterIds == null || parameterIds.isEmpty()) {
            throw new BusinessRuleViolationException("At least one test parameter must be assigned.");
        }

        // Delete existing junction records (soft delete)
        trfTestParameterRepository.softDeleteByTrfIdAndStatus(entity.getId().intValue(), RecordStatus.ACTIVE);

        // Insert new junction records
        for (Integer paramId : parameterIds) {
            TrfTestParameter junction = new TrfTestParameter();
            junction.setTrfId(entity.getId().intValue());
            junction.setParameterId(paramId);
            junction.setIsActiveForTesting(true);
            junction.setStatus(RecordStatus.ACTIVE);
            trfTestParameterRepository.save(junction);
        }
    }

    // --- Business rule validation ---

    private void validateBusinessRules(TrfCreateRequest request) {
        // V-14: expDate > mfgDate
        if (request.getExpDate() != null && request.getMfgDate() != null
                && !request.getExpDate().isAfter(request.getMfgDate())) {
            throw new BusinessRuleViolationException(
                "Expiry date must be after manufacturing date.");
        }

        // V-15: retestDate > mfgDate
        if (request.getRetestDate() != null && request.getMfgDate() != null
                && !request.getRetestDate().isAfter(request.getMfgDate())) {
            throw new BusinessRuleViolationException(
                "Retest date must be after manufacturing date.");
        }

        // V-13: at least one test parameter
        if (request.getTestParameterIds() == null || request.getTestParameterIds().isEmpty()) {
            throw new BusinessRuleViolationException("At least one test parameter must be assigned.");
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // V-11/V-12: Pending_Feasibility -> Accepted OR Rejected only
        if (!"Pending_Feasibility".equals(currentStatus)) {
            throw new BusinessRuleViolationException(
                "Cannot transition from " + currentStatus + " to " + newStatus +
                ". Only Pending_Feasibility can be transitioned.");
        }
        if (!"Accepted".equals(newStatus) && !"Rejected".equals(newStatus)) {
            throw new BusinessRuleViolationException(
                "Invalid transition: Pending_Feasibility -> " + newStatus +
                ". Only Accepted or Rejected are allowed.");
        }
    }

    // --- Entity population helper ---

    private void populateEntity(TrfMaster entity, TrfCreateRequest request) {
        entity.setCompanyName(request.getCompanyName());
        entity.setCompanyAddress(request.getCompanyAddress());
        entity.setManufacturingLicNo(request.getManufacturingLicNo());
        entity.setContactPersonName(request.getContactPersonName());
        entity.setPhoneMobile(request.getPhoneMobile());
        entity.setEmailId(request.getEmailId());
        entity.setProductId(request.getProductId());
        entity.setBatchNumbers(request.getBatchNumbers());
        entity.setCustomerArNo(request.getCustomerArNo());
        entity.setMaterialType(request.getMaterialType());
        entity.setOtherMaterialTypeDesc(request.getOtherMaterialTypeDesc());
        entity.setMfgDate(request.getMfgDate());
        entity.setExpDate(request.getExpDate());
        entity.setRetestDate(request.getRetestDate());
        entity.setBatchSize(request.getBatchSize());
        entity.setSampleQty(request.getSampleQty());
        entity.setPackingCondition(request.getPackingCondition());
        entity.setStorageCondition(request.getStorageCondition());
        entity.setOtherStorageDesc(request.getOtherStorageDesc());
        entity.setIsHygroscopic(request.getIsHygroscopic() != null ? request.getIsHygroscopic() : false);
        entity.setIsLightSensitive(request.getIsLightSensitive() != null ? request.getIsLightSensitive() : false);
        entity.setIsNonHazardous(request.getIsNonHazardous() != null ? request.getIsNonHazardous() : false);
        entity.setIsHazardous(request.getIsHazardous() != null ? request.getIsHazardous() : false);
        entity.setIsMsdsAttached(request.getIsMsdsAttached() != null ? request.getIsMsdsAttached() : false);
        entity.setOtherSampleTypeDesc(request.getOtherSampleTypeDesc());
        entity.setSubmissionType(request.getSubmissionType());
        entity.setSubmissionTypeDesc(request.getSubmissionTypeDesc());
        entity.setAnalysisType(request.getAnalysisType());
        entity.setRegulatoryFormType(request.getRegulatoryFormType());
        entity.setPharmacopoeiaReference(request.getPharmacopoeiaReference());
        entity.setStpDocumentNo(request.getStpDocumentNo());
        entity.setStpVersionNo(request.getStpVersionNo());
        entity.setGrnNumber(request.getGrnNumber());
        entity.setManufacturerId(request.getManufacturerId());
    }

    // --- DTO conversion ---

    private TrfResponse toResponse(TrfMaster entity) {
        List<Integer> paramIds = trfTestParameterRepository
                .findByTrfIdAndStatus(entity.getId().intValue(), RecordStatus.ACTIVE)
                .stream()
                .map(TrfTestParameter::getParameterId)
                .toList();
        return TrfResponse.fromEntity(entity, paramIds);
    }
}
