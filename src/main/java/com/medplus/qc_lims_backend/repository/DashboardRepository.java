package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TrfMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Dashboard aggregation queries across TRF, TestExecution, OOS, and SampleInward tables.
 *
 * Used by DashboardService to compute KPI metrics (SRS Module 12.1).
 * All queries are COUNT-based and safe under read-only transactions.
 *
 * Note: TrfMaster.trfStatus is a String field (not enum), so JPQL uses String literals.
 */
@Repository
public interface DashboardRepository extends JpaRepository<TrfMaster, Long> {

    // --- Inward Requests (R-01) ---

    /**
     * Count TRFs by workflow status.
     * Uses String parameter because TrfMaster.trfStatus is declared as String (not enum).
     */
    @Query("SELECT COUNT(t) FROM TrfMaster t WHERE t.trfStatus = :trfStatus AND t.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE")
    long countByTrfStatus(@Param("trfStatus") String trfStatus);

    // --- Quality Outcomes (R-03) ---

    /**
     * Count samples with ar_status = 'Rejected' in sample_inward_register.
     * This is the definitive "Quality Rejected" metric per plan review C-3.
     */
    @Query("SELECT COUNT(s) FROM com.medplus.qc_lims_backend.domain.SampleInwardRegister s WHERE s.arStatus = 'Rejected' AND s.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE")
    long countRejectedSamples();

    /**
     * Count samples with Hold status in test_execution context.
     * Samples that are Active/For_Information and have associated test_execution
     * records with execution_status = 'Hold'.
     */
    @Query("SELECT COUNT(te) FROM com.medplus.qc_lims_backend.domain.TestExecution te " +
            "WHERE te.executionStatus = 'Hold' " +
            "AND te.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE")
    long countQualityOnHold();
}
