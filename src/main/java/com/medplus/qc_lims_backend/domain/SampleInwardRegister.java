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

/**
 * Sample Inward Register entity.
 * Maps to sample_inward_register table (SRS Module 2).
 *
 * Extends BaseEntity for id, createdAt, updatedAt, createdBy, updatedBy, status.
 */
@Entity
@Table(name = "sample_inward_register")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class SampleInwardRegister extends BaseEntity {

    @Column(name = "inward_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "ar_number", unique = true, length = 50)
    private String arNumber;

    @Column(name = "trf_id")
    private Integer trfId;

    @Column(nullable = false)
    private Integer productId;

    @Column(name = "batch_no", nullable = false, length = 100)
    private String batchNo;

    @Column(name = "received_quantity_packs", nullable = false)
    private Integer receivedQuantityPacks;

    @Column(name = "total_received_units", nullable = false)
    private Integer totalReceivedUnits;

    @Column(name = "storage_location", length = 100)
    private String storageLocation;

    @Column(name = "ar_status", length = 50)
    private String arStatus = "Active";

    @Column(name = "is_subcontracted")
    private Boolean isSubcontracted = false;

    @Column(name = "subcontract_remarks", columnDefinition = "TEXT")
    private String subcontractRemarks;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;
}
