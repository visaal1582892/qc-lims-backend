package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * OOS (Out of Specification) Incident Register entity.
 * Maps to oos_incident_register table (SRS Module 13 / Module 8).
 *
 * Tracks OOS incidents raised when test results fail specification.
 * Dispositions: Pending, Retest_Authorized, Retest_Passed, Batch_Rejected.
 *
 * Phase 6 additions:
 * - retestIncidentId: self-referencing FK for retest chain
 * - rootCause, correctiveAction: RCA fields
 * - revisedResult, revisedResultUnit: final result after retest
 * - oosStatus: workflow status tracking
 */
@Entity
@Table(name = "oos_incident_register")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class OosIncidentRegister extends BaseEntity {

    @Column(name = "incident_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "incident_number", nullable = false, length = 100)
    private String incidentNumber;

    @Column(name = "ar_number", nullable = false, length = 50)
    private String arNumber;

    @Column(name = "execution_id", nullable = false)
    private Integer executionId;

    @Column(name = "raised_by_user_id", nullable = false)
    private Integer raisedByUserId;

    @Column(name = "qc_head_authorized_id")
    private Integer qcHeadAuthorizedId;

    @Column(name = "investigation_summary", columnDefinition = "TEXT")
    private String investigationSummary;

    @Column(name = "retest_attempt_count")
    private Integer retestAttemptCount = 1;

    // --- Product and test context (V16 migration) ---

    /** Product ID associated with this OOS incident */
    @Column(name = "product_id")
    private Integer productId;

    /** Product name (denormalized for display) */
    @Column(name = "product_name", length = 200)
    private String productName;

    /** Batch number of the product under test */
    @Column(name = "batch_no", length = 100)
    private String batchNo;

    /** Test code (e.g. ASSAY, DISSOLUTION) */
    @Column(name = "test_code", length = 50)
    private String testCode;

    /** Specification limit reference (e.g. "98.0-102.0") */
    @Column(name = "specification_reference", length = 500)
    private String specificationReference;

    /** Observed test result value */
    @Column(name = "observed_value", length = 50)
    private String observedValue;

    /** Unit of measurement */
    @Column(name = "unit_of_measurement", length = 50)
    private String unitOfMeasurement;

    /** Name of the test method used */
    @Column(name = "test_method_name", length = 200)
    private String testMethodName;

    /** Description of the deviation */
    @Column(name = "deviation_description", columnDefinition = "TEXT")
    private String deviationDescription;

    // --- OOS workflow tracking ---

    /** Final disposition: Pending, Retest_Authorized, Retest_Passed, Batch_Rejected */
    @Column(name = "final_disposition", length = 50)
    private String finalDisposition;

    /** OOS workflow status (Pending, Investigating, Resolved, Escalated) */
    @Column(name = "oos_status", length = 50)
    private String oosStatus = "Pending";

    // --- Phase 6 retest and resolution ---

    /** Self-referencing FK: if this incident is a retest, links to the parent incident */
    @Column(name = "retest_incident_id")
    private Long retestIncidentId;

    /** Root cause analysis — filled during investigation */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /** Corrective and preventive action — filled during investigation */
    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    /** Final result value after retest or investigation resolution */
    @Column(name = "revised_result", length = 50)
    private String revisedResult;

    /** Unit of measurement for the revised result */
    @Column(name = "revised_result_unit", length = 50)
    private String revisedResultUnit;

    /** Date-time when retest was completed and result finalized */
    @Column(name = "retest_completed_date")
    private LocalDateTime retestCompletedDate;
}
