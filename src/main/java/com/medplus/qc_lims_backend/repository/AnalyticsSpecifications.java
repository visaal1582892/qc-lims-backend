package com.medplus.qc_lims_backend.repository;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification builder for multi-dimensional analytics queries.
 *
 * Composes dynamic WHERE clauses from AnalyticsQueryRequest filter params.
 * Only non-null fields generate predicates — enables flexible partial filtering.
 *
 * C-6 correction: manufacturer_id filter uses subquery through trf_master
 * since sample_inward_register has no manufacturer_id column.
 */
public final class AnalyticsSpecifications {

    private AnalyticsSpecifications() {
        // Utility class — prevent instantiation
    }

    public static Specification<SampleInwardRegister> withBatchNumber(String batchNumber) {
        return (root, query, cb) -> batchNumber != null && !batchNumber.isBlank()
                ? cb.like(cb.lower(root.get("batchNo")), "%" + batchNumber.toLowerCase() + "%")
                : null;
    }

    public static Specification<SampleInwardRegister> withDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SampleInwardRegister> withArStatus(String arStatus) {
        return (root, query, cb) -> arStatus != null && !arStatus.isBlank()
                ? cb.equal(root.get("arStatus"), arStatus)
                : null;
    }

    /**
     * C-6: Filter by manufacturer_id.
     * sample_inward_register has no manufacturer_id; we filter by trf_id that
     * exists in trf_master where manufacturer_id matches.
     * Implemented as a native-level subquery in the service rather than here,
     * since JPA Criteria API subquery for this join path is verbose.
     *
     * For Specification-based approach, we collect matching trf_ids in the service
     * and filter by IN clause here.
     */
    public static Specification<SampleInwardRegister> withManufacturerIdTrfIds(List<Integer> trfIds) {
        return (root, query, cb) -> {
            if (trfIds == null || trfIds.isEmpty()) return null;
            return root.get("trfId").in(trfIds);
        };
    }

    /**
     * Composite specification combining all provided filters.
     * Null filter values are skipped (no predicate generated).
     */
    public static Specification<SampleInwardRegister> composite(
            List<Integer> manufacturerTrfIds,
            String batchNumber,
            LocalDate dateFrom,
            LocalDate dateTo,
            String arStatus) {

        Specification<SampleInwardRegister> spec =
                Specification.where((Specification<SampleInwardRegister>) null);
        spec = spec.and(withManufacturerIdTrfIds(manufacturerTrfIds));
        spec = spec.and(withBatchNumber(batchNumber));
        spec = spec.and(withDateRange(dateFrom, dateTo));
        spec = spec.and(withArStatus(arStatus));
        return spec;
    }
}
