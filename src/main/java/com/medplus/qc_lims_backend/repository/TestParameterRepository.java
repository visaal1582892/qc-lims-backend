package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestParameterRepository extends JpaRepository<TestParameter, Long>, JpaSpecificationExecutor<TestParameter> {

    Page<TestParameter> findByStatus(MasterStatus status, Pageable pageable);

    Page<TestParameter> findByStatus(MasterStatus status, Specification<TestParameter> spec, Pageable pageable);

    Page<TestParameter> findAll(Specification<TestParameter> spec, Pageable pageable);

    @Modifying
    @Query("UPDATE TestParameter tp SET tp.status = :status, tp.updatedBy = :user WHERE tp.id IN :ids")
    int bulkDeactivate(@Param("ids") List<Long> ids, @Param("status") MasterStatus status, @Param("user") String user);
}
