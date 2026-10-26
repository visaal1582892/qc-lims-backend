package com.medplus.qc_lims_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Response for Excel bulk-upload operations.
 * On success: success=true, message contains summary counts.
 * On validation failure: success=false, errorRows contains row→errors mapping,
 *   errorFileBase64 contains the downloadable error report Excel (base64-encoded).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkUploadResponse(
        boolean success,
        String message,
        Map<Integer, List<String>> errorRows,
        String errorFileBase64
) {
    public static BulkUploadResponse success(String message) {
        return new BulkUploadResponse(true, message, null, null);
    }

    public static BulkUploadResponse validationFailure(Map<Integer, List<String>> errors) {
        return new BulkUploadResponse(false, "Validation failed — download error report for details", errors, null);
    }

    public static BulkUploadResponse validationFailureWithFile(Map<Integer, List<String>> errors, byte[] errorFile) {
        String base64 = Base64.getEncoder().encodeToString(errorFile);
        return new BulkUploadResponse(false, "Validation failed — download error report for details", errors, base64);
    }
}
