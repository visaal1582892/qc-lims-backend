package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TrfReceiptChecklist;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TrfReceiptChecklist (physical verification).
 */
@Repository
public interface ReceiptChecklistRepository extends JpaRepository<TrfReceiptChecklist, Long> {

    Optional<TrfReceiptChecklist> findByTrfIdAndStatus(Integer trfId, RecordStatus status);

    List<TrfReceiptChecklist> findByArNumberAndStatus(String arNumber, RecordStatus status);

    Optional<TrfReceiptChecklist> findByIdAndStatus(Long id, RecordStatus status);
}
