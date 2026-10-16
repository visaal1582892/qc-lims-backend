package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TestExecution.
 * Provides status-filtered queries for test execution lifecycle management.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    List<TestExecution> findByStatus(RecordStatus status);

    Optional<TestExecution> findByIdAndStatus(Long id, RecordStatus status);

    List<TestExecution> findByArNumberAndStatus(String arNumber, RecordStatus status);

    List<TestExecution> findByExecutionStatusAndStatus(String executionStatus, RecordStatus status);

    List<TestExecution> findByParameterIdAndStatus(Integer parameterId, RecordStatus status);

    List<TestExecution> findByAllocatedToAnalystIdAndStatus(Integer analystId, RecordStatus status);

    long countByExecutionStatusAndStatus(String executionStatus, RecordStatus status);

    long countByComplianceFlagAndStatus(String complianceFlag, RecordStatus status);

    /**
     * Count records where compliance_flag = 'FAIL' and execution_status is NOT Released or Cancelled_Not_Performed.
     * Used for "Quality Rejected" metric on the dashboard.
     */
    @Query("SELECT COUNT(te) FROM TestExecution te WHERE te.complianceFlag = 'FAIL' AND te.executionStatus NOT IN ('Released', 'Cancelled_Not_Performed') AND te.status = :status")
    long countQualityRejected(@Param("status") RecordStatus status);

    /**
     * Parameter-wise analytics: count tests grouped by parameter.
     */
    @Query("SELECT te.parameterId, COUNT(te) FROM TestExecution te WHERE te.status = :status GROUP BY te.parameterId")
    List<Object[]> countTestsByParameter(@Param("status") RecordStatus status);

    /**
     * Analyst productivity: count tests by analyst within date range.
     */
    @Query("SELECT te.allocatedToAnalystId, COUNT(te) FROM TestExecution te WHERE te.createdAt >= :from AND te.createdAt <= :to AND te.status = :status GROUP BY te.allocatedToAnalystId")
    List<Object[]> countTestsByAnalystAndDateRange(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to, @Param("status") RecordStatus status);

    /**
     * Check for duplicate active allocation: same (ar_number, parameter_id) with status not Released or Cancelled.
     * Used by TestExecutionService.allocateTest() to enforce BR-01.
     */
    boolean existsByArNumberAndParameterIdAndExecutionStatusNotInAndStatus(
            String arNumber, Integer parameterId, java.util.List<String> excludedStatuses, RecordStatus status);

    /**
     * Find AR numbers that have active (non-terminal) test executions.
     * Active = status is ACTIVE and execution_status is NOT Released or Cancelled_Not_Performed.
     * Used by TestExecutionService.getPendingAllocations() to filter out ARs that already have active tests.
     */
    @Query("SELECT DISTINCT te.arNumber FROM TestExecution te WHERE te.status = :status AND te.executionStatus NOT IN ('Released', 'Cancelled_Not_Performed')")
    java.util.List<String> findArNumbersWithActiveExecutions(@Param("status") RecordStatus status);
}
