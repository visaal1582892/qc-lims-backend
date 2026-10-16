package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TrfTestParameter;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for TrfTestParameter (e-TRF to Test Parameter junction).
 */
@Repository
public interface TrfTestParameterRepository extends JpaRepository<TrfTestParameter, Long> {

    List<TrfTestParameter> findByTrfIdAndStatus(Integer trfId, RecordStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE TrfTestParameter t SET t.status = com.medplus.qc_lims_backend.enums.RecordStatus.INACTIVE WHERE t.trfId = :trfId AND t.status = :status")
    void softDeleteByTrfIdAndStatus(@Param("trfId") Integer trfId, @Param("status") RecordStatus status);
}
