package com.medplus.qc_lims_backend.dto;

/**
 * Metric bulk-upload row.
 * Columns: Action (A/U/D), ID, Metric Name, Unit
 */
public record MetricBulkRow(
        Long id,
        String action,
        String metricName,
        String unit,
        int rowNumber
) implements BulkRow {}
