package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MetricSpecificationRepository extends JpaRepository<MetricSpecification, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO metric_specifications_log (metric_specification_id, config_id, metric_id, lower_bound, upper_bound, status, created_by)
            SELECT ms.metric_specification_id, ms.config_id, ms.metric_id, ms.lower_bound, ms.upper_bound, ms.status, :user
            FROM metric_specifications ms
            WHERE ms.metric_specification_id IN (:ids)
            """, nativeQuery = true)
    int logBeforeUpdate(@Param("ids") List<Long> ids, @Param("user") String user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE MetricSpecification ms SET ms.status = :status, ms.updatedBy = :user WHERE ms.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);

    /**
     * Orphan protection lookup (Guardrail 2): Returns config IDs that have active metric specifications.
     */
    @Query("SELECT DISTINCT ms.inventoryConfiguration.id FROM MetricSpecification ms WHERE ms.inventoryConfiguration.id IN :configIds AND ms.status = :status")
    List<Long> findConfigIdsWithActiveMetricSpecs(@Param("configIds") Collection<Long> configIds, @Param("status") MasterStatus status);

    /**
     * Bounded active specs lookup by config IDs for intra-batch and active duplicate validation.
     */
    @Query("SELECT ms FROM MetricSpecification ms WHERE ms.inventoryConfiguration.id IN :configIds AND ms.status = 'A'")
    List<MetricSpecification> findActiveByConfigIds(@Param("configIds") Collection<Long> configIds);
}
