package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Test Parameter & Specification Master entity.
 * Maps to test_parameter_master table (SRS Module 1 / 13).
 *
 * Extends BaseEntity for id, createdAt, updatedAt, createdBy, updatedBy, status.
 */
@Entity
@Table(name = "test_parameter_master")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class TestParameterMaster extends BaseEntity {

    @Column(name = "parameter_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Integer productId;

    @Column(name = "parameter_name", nullable = false, length = 150)
    private String parameterName;

    @Column(name = "standard_reference", length = 50)
    private String standardReference;

    @Column(name = "stp_document_number", length = 100)
    private String stpDocumentNumber;

    @Column(name = "stp_version_number", length = 20)
    private String stpVersionNumber;

    @Column(name = "min_limit", precision = 12, scale = 4)
    private BigDecimal minLimit;

    @Column(name = "max_limit", precision = 12, scale = 4)
    private BigDecimal maxLimit;

    @Column(name = "text_specification", columnDefinition = "TEXT")
    private String textSpecification;

    @Column(name = "is_critical", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isCritical = true;
}
