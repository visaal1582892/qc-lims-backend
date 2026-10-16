package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * OOS (Out of Specification) Incident Register entity.
 * Maps to oos_incident_register table (SRS Module 13, line 399).
 *
 * Tracks OOS incidents raised when test results fail specification.
 * Dispositions: Pending, Retest_Authorized, Batch_Rejected.
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

    @Column(name = "final_disposition", length = 50)
    private String finalDisposition;
}
