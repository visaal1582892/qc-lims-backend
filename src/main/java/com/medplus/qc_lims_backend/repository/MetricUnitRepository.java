package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricUnitRepository extends JpaRepository<MetricUnit, Long> {

    Page<MetricUnit> findByStatus(MasterStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE MetricUnit m SET m.status = :status, m.updatedBy = :user WHERE m.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
