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

    // --- Manufacturer-wise search ---

    /**
     * Find all TRF IDs for a given manufacturer.
     */
    @Query("SELECT t.id FROM TrfMaster t WHERE t.manufacturerId = :manufacturerId AND t.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE")
    java.util.List<Long> findTrfIdsByManufacturerId(@Param("manufacturerId") Integer manufacturerId);

    // --- Date/batch tracking ---

    /**
     * Find samples by date range and optionally batch number.
     * Returns distinct sample_inward_register entries.
     */
    @Query("SELECT DISTINCT s FROM com.medplus.qc_lims_backend.domain.SampleInwardRegister s " +
            "WHERE s.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE " +
            "AND (:dateFrom IS NULL OR s.receiptDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR s.receiptDate <= :dateTo) " +
            "AND (:batchNo IS NULL OR s.batchNo LIKE CONCAT('%', :batchNo, '%'))")
    java.util.List<com.medplus.qc_lims_backend.domain.SampleInwardRegister> findByDateRangeAndBatch(
            @Param("dateFrom") java.time.LocalDate dateFrom,
            @Param("dateTo") java.time.LocalDate dateTo,
            @Param("batchNo") String batchNo);

    // --- Parameter analytics ---

    /**
     * Count test executions grouped by parameter ID for a specific parameter.
     */
    @Query("SELECT te.complianceFlag, COUNT(te) FROM TestExecution te " +
            "WHERE te.parameterId = :parameterId AND te.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE " +
            "GROUP BY te.complianceFlag")
    java.util.List<Object[]> countByParameterAndCompliance(@Param("parameterId") Integer parameterId);

    // --- Status-based filtering ---

    /**
     * Count samples grouped by lifecycle status (ar_status).
     */
    @Query("SELECT s.arStatus, COUNT(s) FROM com.medplus.qc_lims_backend.domain.SampleInwardRegister s " +
            "WHERE s.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE " +
            "GROUP BY s.arStatus")
    java.util.List<Object[]> countByArStatusGroups();

    /**
     * Find all active samples filtered by a specific ar_status.
     */
    @Query("SELECT s FROM com.medplus.qc_lims_backend.domain.SampleInwardRegister s " +
            "WHERE s.arStatus = :status AND s.status = com.medplus.qc_lims_backend.enums.RecordStatus.ACTIVE")
    java.util.List<com.medplus.qc_lims_backend.domain.SampleInwardRegister> findByArStatus(@Param("status") String status);
}
