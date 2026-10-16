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

import com.medplus.qc_lims_backend.enums.ArStatus;
import jakarta.persistence.Convert;
import java.math.BigDecimal;
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

    @Column(name = "ar_status", nullable = false, length = 32)
    private String arStatus = "REGISTERED";

    @Column(name = "test_completion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal testCompletionRate = BigDecimal.ZERO;

    @Column(name = "is_subcontracted")
    private Boolean isSubcontracted = false;

    @Column(name = "subcontract_remarks", columnDefinition = "TEXT")
    private String subcontractRemarks;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    /** Cross-reference to For_Information AR number (Phase 6) — stores the AR number of the source AR when this is a fresh analysis / retest linked to another AR */
    @Column(name = "linked_for_info_ar_number", length = 50)
    private String linkedForInfoArNumber;

    // --- AR lifecycle helpers (instance methods) ---

    /**
     * Return the ArStatus enum for this entity's ar_status.
     */
    public ArStatus getArStatusEnum() {
        return ArStatus.fromDbValue(this.arStatus);
    }

    /**
     * Convenience: set ar_status from an enum value.
     */
    public void setArStatusEnum(ArStatus status) {
        this.arStatus = status.getDbValue();
    }
}
