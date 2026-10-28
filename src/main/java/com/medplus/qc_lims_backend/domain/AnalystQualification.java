package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.MasterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Analyst Qualification Master — goal.md §1.4.
 */
@Entity
@Table(name = "analyst_qualification_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AnalystQualification extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qualification_id")
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "param_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TestParameter testParameter;

    @Column(name = "qualified_on", nullable = false)
    private LocalDate qualifiedOn;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private MasterStatus status = MasterStatus.ACTIVE;
}
