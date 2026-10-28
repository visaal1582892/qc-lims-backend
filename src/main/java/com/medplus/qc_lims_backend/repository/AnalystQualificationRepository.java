package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalystQualificationRepository extends JpaRepository<AnalystQualification, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO analyst_qualification_master_log (qualification_id, employee_id, param_id, qualified_on, status, created_by)
            SELECT aq.qualification_id, aq.employee_id, aq.param_id, aq.qualified_on, aq.status, :user
            FROM analyst_qualification_master aq
            WHERE aq.qualification_id IN (:ids)
            """, nativeQuery = true)
    int logBeforeUpdate(@Param("ids") List<Long> ids, @Param("user") String user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE AnalystQualification aq SET aq.status = :status, aq.updatedBy = :user WHERE aq.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
