package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.UserEntity;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Rules.md §3.1 — Pure Spring Data JPA repository.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmployeeIdAndStatus(String employeeId, RecordStatus status);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.userId = :id")
    Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);

    Page<UserEntity> findByStatus(RecordStatus status, Pageable pageable);

    Page<UserEntity> findAll(Pageable pageable);
}
