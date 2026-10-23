package com.medplus.qc_lims_backend.dto;

/**
 * Inventory Configuration bulk-upload row.
 * Columns: Action (A/U/D), ID, Product ID, Parameter ID, Count Needed
 */
public record InventoryConfigurationBulkRow(
        Long id,
        String action,
        String productId,
        Long parameterId,
        Integer countNeeded,
        int rowNumber
) {}
