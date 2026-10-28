package com.medplus.qc_lims_backend.dto;

/**
 * Inventory Configuration bulk-upload row.
 * Columns: Action (A/U/D), ID, Product ID, Parameter ID, Test Method ID, Is Critical (Y/N), Count Needed, Result Type (METRICS/TEXT), Specification Text
 */
public record InventoryConfigurationBulkRow(
        Long id,
        String action,
        String productId,
        Long parameterId,
        String testMethodId,
        String isCritical,
        Integer countNeeded,
        String resultType,
        String specificationText,
        int rowNumber
) implements BulkRow {}
