package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.MasterStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Metric Master (formerly Metric Unit Master).
 */
@Entity
@Table(name = "metric_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Metric extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long id;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "unit", length = 100)
    private String unit;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private MasterStatus status = MasterStatus.ACTIVE;
}
