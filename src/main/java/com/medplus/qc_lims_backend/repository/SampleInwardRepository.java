package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SampleInwardRegister.
 * All queries filter by RecordStatus.ACTIVE (consistent with Phase 1 pattern).
 */
@Repository
public interface SampleInwardRepository extends JpaRepository<SampleInwardRegister, Long> {

    List<SampleInwardRegister> findByStatus(RecordStatus status);

    Optional<SampleInwardRegister> findByIdAndStatus(Long id, RecordStatus status);

    Optional<SampleInwardRegister> findByArNumberAndStatus(String arNumber, RecordStatus status);

    List<SampleInwardRegister> findByProductIdAndStatus(Integer productId, RecordStatus status);

    List<SampleInwardRegister> findByBatchNoContainingIgnoreCaseAndStatus(String batchNo, RecordStatus status);

    List<SampleInwardRegister> findByTrfIdAndStatus(Integer trfId, RecordStatus status);

    long countByStatus(RecordStatus status);
}
