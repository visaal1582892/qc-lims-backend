package com.medplus.qc_lims_backend.dto;

/**
 * Test Parameter bulk-upload row.
 * Columns: Action (A/U/D), ID, Name
 */
public record TestParameterBulkRow(
        Long id,
        String action,
        String name,
        int rowNumber
) implements BulkRow {}
