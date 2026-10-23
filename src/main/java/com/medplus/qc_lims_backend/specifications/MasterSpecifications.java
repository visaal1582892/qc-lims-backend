package com.medplus.qc_lims_backend.specifications;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Shared JPA Specification helpers for master data search.
 * goal.md §4.2 — Column-level search/filtering.
 */
public final class MasterSpecifications {

    private MasterSpecifications() {}

    /**
     * Case-insensitive LIKE search on the "name" field.
     */
    public static <T> Specification<T> nameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("name")),
                "%" + keyword.trim().toLowerCase() + "%"
        );
    }

    /**
     * Case-insensitive LIKE search on the "employeeId" field.
     */
    public static <T> Specification<T> employeeIdContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("employeeId")),
                "%" + keyword.trim().toLowerCase() + "%"
        );
    }

    /**
     * Case-insensitive LIKE search on the "productId" field.
     */
    public static <T> Specification<T> productIdContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("productId")),
                "%" + keyword.trim().toLowerCase() + "%"
        );
    }

    /**
     * Combines name and subName search for MetricUnit.
     */
    public static <T> Specification<T> nameOrSubNameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("subName")), pattern)
        );
    }
}
