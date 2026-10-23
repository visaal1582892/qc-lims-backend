package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.MasterStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * QC Inventory Configuration — goal.md §1.5.
 */
@Entity
@Table(name = "qc_inventory_configuration", uniqueConstraints = {
    @UniqueConstraint(name = "uq_prod_param_status", columnNames = {"product_id", "param_id", "status"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class InventoryConfiguration extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long id;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "param_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TestParameter testParameter;

    @Column(name = "count_needed", nullable = false)
    private Integer countNeeded;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private MasterStatus status = MasterStatus.ACTIVE;
}
