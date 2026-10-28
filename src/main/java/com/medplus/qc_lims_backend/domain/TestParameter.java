package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.MasterStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Test Parameter Master — goal.md §1.3.
 */
@Entity
@Table(name = "test_parameter_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class TestParameter extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "param_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private MasterStatus status = MasterStatus.ACTIVE;
}
