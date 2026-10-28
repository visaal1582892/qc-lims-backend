package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryConfigurationRepository extends JpaRepository<InventoryConfiguration, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO qc_inventory_configuration_log (config_id, product_id, param_id, test_method_id, is_critical, result_type, specification_text, count_needed, status, created_by)
            SELECT ic.config_id, ic.product_id, ic.param_id, ic.test_method_id, ic.is_critical, ic.result_type, ic.specification_text, ic.count_needed, ic.status, :user
            FROM qc_inventory_configuration ic
            WHERE ic.config_id IN (:ids)
            """, nativeQuery = true)
    int logBeforeUpdate(@Param("ids") List<Long> ids, @Param("user") String user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE InventoryConfiguration ic SET ic.status = :status, ic.updatedBy = :user WHERE ic.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
