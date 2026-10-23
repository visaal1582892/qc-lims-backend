package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.RoleEntity;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleCodeAndStatus(String roleCode, RecordStatus status);

    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.rights WHERE r.roleId = :id")
    Optional<RoleEntity> findByIdWithRights(@Param("id") Long id);

    Page<RoleEntity> findByStatus(RecordStatus status, Pageable pageable);
}
