package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO metric_master_log (metric_id, metric_name, unit, status, created_by)
            SELECT m.metric_id, m.metric_name, m.unit, m.status, :user
            FROM metric_master m
            WHERE m.metric_id IN (:ids)
            """, nativeQuery = true)
    int logBeforeUpdate(@Param("ids") List<Long> ids, @Param("user") String user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Metric m SET m.status = :status, m.updatedBy = :user WHERE m.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
