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
 * TRF-to-Test-Parameter junction entity.
 * Maps to trf_test_parameters table.
 *
 * Uses surrogate id (per OQ-05) to preserve BaseEntity inheritance.
 * UNIQUE constraint on (trf_id, parameter_id) enforces junction semantics.
 */
@Entity
@Table(name = "trf_test_parameters")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TrfTestParameter extends BaseEntity {

    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "trf_id", nullable = false)
    private Integer trfId;

    @Column(name = "parameter_id", nullable = false)
    private Integer parameterId;

    @Column(name = "is_active_for_testing")
    private Boolean isActiveForTesting = true;
}
