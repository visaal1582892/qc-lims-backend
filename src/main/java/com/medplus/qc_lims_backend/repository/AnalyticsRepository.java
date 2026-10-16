package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Analytics queries for multi-dimensional search (SRS Module 12.2).
 *
 * Extends JpaSpecificationExecutor for dynamic WHERE-clause composition.
 * Base entity is SampleInwardRegister — analytics searches by sample,
 * joining with test_execution for execution-level details.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<SampleInwardRegister, Long>,
        JpaSpecificationExecutor<SampleInwardRegister> {

    /**
     * Count samples by ar_status (Active, For_Information, Closed, Rejected).
     */
    @Query("SELECT COUNT(s) FROM SampleInwardRegister s WHERE s.arStatus = :arStatus AND s.status = :status")
    long countByArStatus(@Param("arStatus") String arStatus, @Param("status") RecordStatus status);

    /**
     * Distinct manufacturer IDs for the analytics dimension dropdown.
     */
    @Query("SELECT DISTINCT t.manufacturerId FROM TrfMaster t WHERE t.status = :status ORDER BY t.manufacturerId")
    List<Integer> findDistinctManufacturerIds(@Param("status") RecordStatus status);
}
