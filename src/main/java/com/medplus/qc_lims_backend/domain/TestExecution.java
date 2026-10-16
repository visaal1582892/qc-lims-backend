package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Test Allocation & Execution entity.
 * Maps to test_execution table (SRS Module 13, line 373).
 *
 * Tracks individual test execution lifecycle per parameter per sample.
 * Execution statuses: Allocated, Testing, Hold, Cancelled_Not_Performed,
 *                      Under_Review, Rework_Requested, Released
 */
@Entity
@Table(name = "test_execution")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TestExecution extends BaseEntity {

    @Column(name = "execution_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "ar_number", length = 50)
    private String arNumber;

    @Column(name = "parameter_id", nullable = false)
    private Integer parameterId;

    @Column(name = "allocated_to_analyst_id", nullable = false)
    private Integer allocatedToAnalystId;

    @Column(name = "assigned_by_qc_head_id", nullable = false)
    private Integer assignedByQcHeadId;

    @Column(name = "allocated_units", nullable = false)
    private Integer allocatedUnits;

    @Column(name = "consumed_units")
    private Integer consumedUnits = 0;

    @Column(name = "execution_status", length = 50)
    private String executionStatus = "Allocated";

    @Column(name = "hold_or_cancel_reason", columnDefinition = "TEXT")
    private String holdOrCancelReason;

    @Column(name = "rework_reason", columnDefinition = "TEXT")
    private String reworkReason;

    @Column(name = "result_numeric_value", precision = 12, scale = 4)
    private BigDecimal resultNumericValue;

    @Column(name = "result_text_value", length = 255)
    private String resultTextValue;

    @Column(name = "validity_status", length = 20)
    private String validityStatus = "Valid";

    @Column(name = "compliance_flag", length = 20)
    private String complianceFlag;

    @Column(name = "roa_file_path", columnDefinition = "TEXT")
    private String roaFilePath;

    @Column(name = "coa_file_path", columnDefinition = "TEXT")
    private String coaFilePath;

    @Column(name = "review_status", length = 32)
    private String reviewStatus;

    @Column(name = "qc_head_reviewed_at")
    private Instant qcHeadReviewedAt;

    @Column(name = "qa_head_reviewed_at")
    private Instant qaHeadReviewedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
