package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * e-TRF Receipt Checklist entity.
 * Maps to trf_receipt_checklist table (SRS 3.1 D).
 *
 * Extends BaseEntity for id, createdAt, updatedAt, createdBy, updatedBy, status.
 */
@Entity
@Table(name = "trf_receipt_checklist")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TrfReceiptChecklist extends BaseEntity {


    @Column(name = "trf_id", nullable = false)
    private Integer trfId;

    @Column(name = "ar_number", length = 50)
    private String arNumber;

    @Column(name = "match_sample_name_batch", nullable = false, length = 10)
    private String matchSampleNameBatch;

    @Column(name = "free_from_damage_leakage", nullable = false, length = 10)
    private String freeFromDamageLeakage;

    @Column(name = "pharmacopoeial_ref_mentioned", nullable = false, length = 10)
    private String pharmacopoeialRefMentioned;

    @Column(name = "verified_by_user_id", nullable = false)
    private Integer verifiedByUserId;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
