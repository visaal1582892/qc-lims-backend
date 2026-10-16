package com.medplus.qc_lims_backend.dto.response;

import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.enums.RecordStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only response projection for TrfMaster.
 * Includes nested testParameters list from junction table.
 */
public record TrfResponse(
    Long id,
    String grnNumber,
    Integer manufacturerId,
    Integer productId,
    String batchNumbers,
    String customerArNo,
    String materialType,
    String otherMaterialTypeDesc,
    LocalDate mfgDate,
    LocalDate expDate,
    LocalDate retestDate,
    Integer batchSize,
    Integer sampleQty,
    String packingCondition,
    String storageCondition,
    String otherStorageDesc,
    Boolean isHygroscopic,
    Boolean isLightSensitive,
    Boolean isNonHazardous,
    Boolean isHazardous,
    Boolean isMsdsAttached,
    String otherSampleTypeDesc,
    String submissionType,
    String submissionTypeDesc,
    String analysisType,
    String regulatoryFormType,
    String pharmacopoeiaReference,
    String stpDocumentNo,
    String stpVersionNo,
    String companyName,
    String companyAddress,
    String manufacturingLicNo,
    String contactPersonName,
    String phoneMobile,
    String emailId,
    String trfStatus,
    Integer submittedByUserId,
    LocalDateTime submittedAt,
    List<Integer> testParameterIds,
    RecordStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static TrfResponse fromEntity(TrfMaster entity, List<Integer> parameterIds) {
        return new TrfResponse(
            entity.getId(),
            entity.getGrnNumber(),
            entity.getManufacturerId(),
            entity.getProductId(),
            entity.getBatchNumbers(),
            entity.getCustomerArNo(),
            entity.getMaterialType(),
            entity.getOtherMaterialTypeDesc(),
            entity.getMfgDate(),
            entity.getExpDate(),
            entity.getRetestDate(),
            entity.getBatchSize(),
            entity.getSampleQty(),
            entity.getPackingCondition(),
            entity.getStorageCondition(),
            entity.getOtherStorageDesc(),
            entity.getIsHygroscopic(),
            entity.getIsLightSensitive(),
            entity.getIsNonHazardous(),
            entity.getIsHazardous(),
            entity.getIsMsdsAttached(),
            entity.getOtherSampleTypeDesc(),
            entity.getSubmissionType(),
            entity.getSubmissionTypeDesc(),
            entity.getAnalysisType(),
            entity.getRegulatoryFormType(),
            entity.getPharmacopoeiaReference(),
            entity.getStpDocumentNo(),
            entity.getStpVersionNo(),
            entity.getCompanyName(),
            entity.getCompanyAddress(),
            entity.getManufacturingLicNo(),
            entity.getContactPersonName(),
            entity.getPhoneMobile(),
            entity.getEmailId(),
            entity.getTrfStatus(),
            entity.getSubmittedByUserId(),
            entity.getSubmittedAt(),
            parameterIds,
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
