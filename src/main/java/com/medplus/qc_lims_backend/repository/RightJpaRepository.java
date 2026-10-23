package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.RightEntity;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RightJpaRepository extends JpaRepository<RightEntity, String> {

    Page<RightEntity> findByStatus(RecordStatus status, Pageable pageable);
}
