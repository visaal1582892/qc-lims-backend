package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.OosIncidentRegister;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for OosIncidentRegister.
 * Tracks OOS incidents raised when test results fail specification.
 */
@Repository
public interface OosIncidentRepository extends JpaRepository<OosIncidentRegister, Long> {

    List<OosIncidentRegister> findByStatus(RecordStatus status);

    Optional<OosIncidentRegister> findByIdAndStatus(Long id, RecordStatus status);

    List<OosIncidentRegister> findByArNumberAndStatus(String arNumber, RecordStatus status);

    List<OosIncidentRegister> findByFinalDispositionAndStatus(String disposition, RecordStatus status);

    /**
     * Count active OOS incidents: those with disposition = 'Pending' or 'Retest_Authorized'.
     */
    @Query("SELECT COUNT(o) FROM OosIncidentRegister o WHERE o.finalDisposition IN ('Pending', 'Retest_Authorized') AND o.status = :status")
    long countActiveOosIncidents(@Param("status") RecordStatus status);
}
