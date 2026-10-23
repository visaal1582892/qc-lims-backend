package com.medplus.qc_lims_backend.dto;

/**
 * Generic Excel bulk-upload row — goal.md §3.
 * Every master row contains: Action (A/U/D), ID, rowNumber (for error tracking).
 */
public record BulkRow(
        Long id,
        String action,
        int rowNumber
) {}
