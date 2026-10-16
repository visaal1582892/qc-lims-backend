package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.TestParameterMaster;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TestParameterMaster.
 * Provides CRUD and custom query methods for test parameter lookups.
 */
@Repository
public interface TestParameterMasterRepository extends JpaRepository<TestParameterMaster, Long> {

    List<TestParameterMaster> findByStatus(RecordStatus status);

    List<TestParameterMaster> findByProductIdAndStatus(Integer productId, RecordStatus status);

    List<TestParameterMaster> findByParameterNameContainingIgnoreCaseAndStatus(String name, RecordStatus status);

    List<TestParameterMaster> findByProductIdAndParameterNameContainingIgnoreCaseAndStatus(
            Integer productId, String name, RecordStatus status);

    boolean existsByProductIdAndParameterNameAndStatus(Integer productId, String parameterName, RecordStatus status);

    boolean existsByProductIdAndParameterNameAndIdNotAndStatus(
            Integer productId, String parameterName, Long id, RecordStatus status);

    Optional<TestParameterMaster> findByIdAndStatus(Long id, RecordStatus status);

    List<TestParameterMaster> findByIdInAndStatus(java.util.Collection<Long> ids, RecordStatus status);
}
