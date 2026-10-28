package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.MasterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Metric Specifications — Child master of QC Inventory Configuration.
 */
@Entity
@Table(name = "metric_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class MetricSpecification extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_specification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private InventoryConfiguration inventoryConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Metric metric;

    @Column(name = "lower_bound", precision = 12, scale = 2)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", precision = 12, scale = 2)
    private BigDecimal upperBound;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private MasterStatus status = MasterStatus.ACTIVE;
}
