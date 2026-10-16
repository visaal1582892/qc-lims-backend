package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Subcontract Register entity (SRS Module 10).
 *
 * Tracks samples dispatched to external laboratories.
 * Subcontracted samples do NOT get an AR number (ar_number is NULL).
 */
@Entity
@Table(name = "subcontract_register")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class SubcontractRegister extends BaseEntity {

    @Column(name = "subcontract_no", nullable = false, length = 100)
    @EqualsAndHashCode.Include
    private String subcontractNo;

    /** TRF reference — may link back to the original TRF record */
    @Column(name = "trf_number", length = 100)
    private String trfNumber;

    /** AR number — NULL for subcontracted samples per business rule */
    @Column(name = "ar_number", length = 50)
    private String arNumber;

    /** External lab name (required) */
    @Column(name = "external_lab_name", nullable = false, length = 255)
    private String externalLabName;

    /** Reason for subcontracting (required) */
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    /** Date the sample was dispatched to the external lab */
    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    /** QC Head who authorized the dispatch */
    @Column(name = "dispatched_by_qc_head_id", nullable = false)
    private Long dispatchedByQcHeadId;

    /** Expected date of return from external lab */
    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    /** Subcontract status: DISPATCHED, IN_PROGRESS, COMPLETED, CANCELLED */
    @Column(name = "subcontract_status", nullable = false, length = 50)
    private String subcontractStatus = "DISPATCHED";

    /** Additional remarks or status updates */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /** Flag indicating whether the ROA has been received from the external lab */
    @Column(name = "roa_received")
    private Boolean roaReceived = false;

    // --- Helpers ---

    /** Return the SubcontractStatus enum for this entity's status. */
    public com.medplus.qc_lims_backend.enums.SubcontractStatus getStatusEnum() {
        return com.medplus.qc_lims_backend.enums.SubcontractStatus.fromDbValue(this.subcontractStatus);
    }

    /** Convenience: set status from an enum value. */
    public void setStatusEnum(com.medplus.qc_lims_backend.enums.SubcontractStatus status) {
        this.subcontractStatus = status.getDbValue();
    }
}
