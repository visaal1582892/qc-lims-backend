package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * e-TRF Master entity.
 * Maps to trf_master table (SRS Module 3).
 *
 * Extends BaseEntity for id, createdAt, updatedAt, createdBy, updatedBy, status.
 */
@Entity
@Table(name = "trf_master")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TrfMaster extends BaseEntity {

    @Column(name = "trf_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "grn_number", length = 100)
    private String grnNumber;

    @Column(name = "manufacturer_id", nullable = false)
    private Integer manufacturerId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "batch_numbers", nullable = false, columnDefinition = "TEXT")
    private String batchNumbers;

    @Column(name = "customer_ar_no", length = 100)
    private String customerArNo;

    @Column(name = "material_type", length = 100)
    private String materialType;

    @Column(name = "other_material_type_desc", length = 150)
    private String otherMaterialTypeDesc;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;

    @Column(name = "retest_date", nullable = false)
    private LocalDate retestDate;

    @Column(name = "batch_size")
    private Integer batchSize;

    @Column(name = "sample_qty", nullable = false)
    private Integer sampleQty;

    @Column(name = "packing_condition", length = 150)
    private String packingCondition;

    @Column(name = "storage_condition", nullable = false, length = 100)
    private String storageCondition;

    @Column(name = "other_storage_desc", length = 150)
    private String otherStorageDesc;

    @Column(name = "is_hygroscopic")
    private Boolean isHygroscopic = false;

    @Column(name = "is_light_sensitive")
    private Boolean isLightSensitive = false;

    @Column(name = "is_non_hazardous")
    private Boolean isNonHazardous = false;

    @Column(name = "is_hazardous")
    private Boolean isHazardous = false;

    @Column(name = "is_msds_attached")
    private Boolean isMsdsAttached = false;

    @Column(name = "other_sample_type_desc", length = 150)
    private String otherSampleTypeDesc;

    @Column(name = "submission_type", length = 50)
    private String submissionType;

    @Column(name = "submission_type_desc", length = 150)
    private String submissionTypeDesc;

    @Column(name = "analysis_type", nullable = false, length = 100)
    private String analysisType;

    @Column(name = "regulatory_form_type", length = 100)
    private String regulatoryFormType;

    @Column(name = "pharmacopoeia_reference", nullable = false, length = 50)
    private String pharmacopoeiaReference;

    @Column(name = "stp_document_no", length = 100)
    private String stpDocumentNo;

    @Column(name = "stp_version_no", length = 50)
    private String stpVersionNo;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Column(name = "manufacturing_lic_no", nullable = false, length = 100)
    private String manufacturingLicNo;

    @Column(name = "contact_person_name", length = 150)
    private String contactPersonName;

    @Column(name = "phone_mobile", length = 50)
    private String phoneMobile;

    @Column(name = "email_id", length = 150)
    private String emailId;

    @Column(name = "trf_status", length = 50)
    private String trfStatus = "Pending_Feasibility";

    @Column(name = "submitted_by_user_id", nullable = false)
    private Integer submittedByUserId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
