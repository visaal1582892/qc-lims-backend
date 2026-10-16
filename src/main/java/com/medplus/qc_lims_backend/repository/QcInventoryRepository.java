package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.QcInventory;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

/**
 * Spring Data JPA repository for QcInventory.
 * All queries filter by RecordStatus.ACTIVE (consistent with Phase 1-2 pattern).
 */
@Repository
public interface QcInventoryRepository extends JpaRepository<QcInventory, Long> {

    Optional<QcInventory> findByIdAndStatus(Long id, RecordStatus status);

    Optional<QcInventory> findByArNumberAndStatus(String arNumber, RecordStatus status);

    boolean existsByArNumberAndStatus(String arNumber, RecordStatus status);

    List<QcInventory> findByProductIdAndStatus(Integer productId, RecordStatus status);

    List<QcInventory> findByManufacturerIdAndStatus(Integer manufacturerId, RecordStatus status);

    List<QcInventory> findByBatchNoContainingIgnoreCaseAndStatus(String batchNo, RecordStatus status);

    List<QcInventory> findByStorageLocationAndStatus(String storageLocation, RecordStatus status);

    List<QcInventory> findByProductIdAndBatchNoAndStatus(Integer productId, String batchNo, RecordStatus status);

    List<QcInventory> findByStatus(RecordStatus status);

    /** Find items where allocated > consumed (unconsumed variance) */
    List<QcInventory> findByQuantityAllocatedGreaterThanAndStatus(Integer consumed, RecordStatus status);

    /** Low stock alert: current_balance computed column threshold */
    List<QcInventory> findByCurrentBalanceLessThanEqualAndStatus(int threshold, RecordStatus status);

    /** PESSIMISTIC_WRITE lock for concurrent allocation */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QcInventory> findByIdAndStatusForUpdate(Long id, RecordStatus status);

    long countByStatus(RecordStatus status);

    long countByStorageLocationAndStatus(String storageLocation, RecordStatus status);
}
