package com.medplus.qc_lims_backend.dto;

import java.math.BigDecimal;

/**
 * Metric Specification bulk-upload row.
 * Columns: ID, Config ID, Metric ID, Lower Bound, Upper Bound, Action (A/U/D)
 */
public record MetricSpecificationBulkRow(
        Long id,
        String action,
        Long configId,
        Long metricId,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        int rowNumber
) implements BulkRow {
    // Backward compatibility
    public Long metricUnitId() {
        return metricId;
    }
}
