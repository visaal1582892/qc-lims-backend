package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
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
public interface InventoryConfigurationRepository extends JpaRepository<InventoryConfiguration, Long> {

    Page<InventoryConfiguration> findByStatus(MasterStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE InventoryConfiguration ic SET ic.status = :status, ic.updatedBy = :user WHERE ic.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
