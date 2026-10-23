package com.medplus.qc_lims_backend.dto;

/**
 * Metric Unit bulk-upload row.
 * Columns: Action (A/U/D), ID, Name, Sub Name
 */
public record MetricUnitBulkRow(
        Long id,
        String action,
        String name,
        String subName,
        int rowNumber
) implements BulkRow {}
