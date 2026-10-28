package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestParameterRepository extends JpaRepository<TestParameter, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO test_parameter_master_log (param_id, name, status, created_by)
            SELECT tp.param_id, tp.name, tp.status, :user
            FROM test_parameter_master tp
            WHERE tp.param_id IN (:ids)
            """, nativeQuery = true)
    int logBeforeUpdate(@Param("ids") List<Long> ids, @Param("user") String user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TestParameter tp SET tp.status = :status, tp.updatedBy = :user WHERE tp.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
