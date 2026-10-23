package com.medplus.qc_lims_backend.dto;

/**
 * Test Parameter bulk-upload row.
 * Columns: Action (A/U/D), ID, Name, Result Type (TEXTUAL/METRICS), Metric Unit ID
 */
public record TestParameterBulkRow(
        Long id,
        String action,
        String name,
        String resultType,
        Long metricUnitId,
        int rowNumber
) {}
