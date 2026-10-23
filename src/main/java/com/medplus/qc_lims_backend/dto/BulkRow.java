package com.medplus.qc_lims_backend.dto;

/**
 * Marker interface for Excel bulk-upload row DTOs.
 * Every master row exposes: Action (A/U/D), ID, rowNumber (for error tracking).
 * Implemented by domain-specific BulkRow records.
 */
public interface BulkRow {

    Long id();

    String action();

    int rowNumber();
}
