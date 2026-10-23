package com.medplus.qc_lims_backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Response for Excel bulk-upload operations.
 * On success: success=true, message contains summary counts.
 * On validation failure: success=false, errorRows contains row→errors mapping,
 *   errorFileBytes contains the downloadable error report Excel.
 */
public record BulkUploadResponse(
        boolean success,
        String message,
        Map<Integer, List<String>> errorRows,
        byte[] errorFileBytes
) {
    public static BulkUploadResponse success(String message) {
        return new BulkUploadResponse(true, message, null, null);
    }

    public static BulkUploadResponse validationFailure(Map<Integer, List<String>> errors) {
        return new BulkUploadResponse(false, "Validation failed — download error report for details", errors, null);
    }

    public static BulkUploadResponse validationFailureWithFile(Map<Integer, List<String>> errors, byte[] errorFile) {
        return new BulkUploadResponse(false, "Validation failed — download error report for details", errors, errorFile);
    }
}
