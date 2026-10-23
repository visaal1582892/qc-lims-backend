package com.medplus.qc_lims_backend.dto;

/**
 * Analyst Qualification bulk-upload row.
 * Columns: Action (A/U/D), ID, Employee ID, Parameter ID, Qualified On (YYYY-MM-DD)
 */
public record AnalystQualificationBulkRow(
        Long id,
        String action,
        String employeeId,
        Long parameterId,
        String qualifiedOn,
        int rowNumber
) implements BulkRow {}
