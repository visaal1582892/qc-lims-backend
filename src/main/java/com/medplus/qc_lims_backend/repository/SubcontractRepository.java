package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.SubcontractRegister;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for SubcontractRegister.
 * Tracks external lab dispatches (SRS Module 10).
 */
@Repository
public interface SubcontractRepository extends JpaRepository<SubcontractRegister, Long> {

    /** Find all active subcontracts ordered by dispatch date descending. */
    List<SubcontractRegister> findByStatusOrderByDispatchDateDesc(RecordStatus status);

    /** Find subcontracts by TRF ID and active status. */
    List<SubcontractRegister> findByTrfIdAndStatus(Integer trfId, RecordStatus status);

    /** Find subcontract by subcontract number and active status. */
    SubcontractRegister findBySubcontractNoAndStatus(String subcontractNo, RecordStatus status);

    /** Count subcontracts by workflow status (active only). */
    long countBySubcontractStatusAndStatus(String subcontractStatus, RecordStatus status);

    /** Find subcontracts by workflow status (active only). */
    List<SubcontractRegister> findBySubcontractStatusAndStatusOrderByDispatchDateDesc(String subcontractStatus, RecordStatus status);
}
