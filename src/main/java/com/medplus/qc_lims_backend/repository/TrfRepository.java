package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TrfMaster.
 * Provides status-filtered queries for e-TRF lookups.
 */
@Repository
public interface TrfRepository extends JpaRepository<TrfMaster, Long> {

    List<TrfMaster> findByStatus(RecordStatus status);

    Optional<TrfMaster> findByIdAndStatus(Long id, RecordStatus status);

    List<TrfMaster> findByTrfStatusAndStatus(String trfStatus, RecordStatus status);

    List<TrfMaster> findByProductIdAndStatus(Integer productId, RecordStatus status);

    List<TrfMaster> findByGrnNumberContainingIgnoreCaseAndStatus(String grnNumber, RecordStatus status);

    List<TrfMaster> findByManufacturerIdAndStatus(Integer manufacturerId, RecordStatus status);
}
