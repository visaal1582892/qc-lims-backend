package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TrfTestParameter;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TrfTestParameter (e-TRF to Test Parameter junction).
 */
@Repository
public interface TrfTestParameterRepository extends JpaRepository<TrfTestParameter, Long> {

    List<TrfTestParameter> findByTrfIdAndStatus(Integer trfId, RecordStatus status);

    @Modifying
    @Query("DELETE FROM TrfTestParameter t WHERE t.trfId = :trfId AND t.status = :status")
    void deleteByTrfIdAndStatus(@Param("trfId") Integer trfId, @Param("status") RecordStatus status);
}
