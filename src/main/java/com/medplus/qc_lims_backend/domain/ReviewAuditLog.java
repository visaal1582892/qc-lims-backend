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

import java.time.Instant;

/**
 * Review Audit Log entity — immutable audit trail of review decisions.
 *
 * Each row records one review decision (QC Head or QA Head) with reviewer,
 * decision, optional reason, and timestamp. This table is append-only.
 */
@Entity
@Table(name = "review_audit_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class ReviewAuditLog extends BaseEntity {


    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "review_stage", nullable = false, length = 20)
    private String reviewStage;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
