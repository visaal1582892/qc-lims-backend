package com.medplus.qc_lims_backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for barcode label data.
 * Format: AR_Number|Product_Name|Batch_Number|Storage_Condition (pipe-delimited).
 * Per SRS Module 4 line 123.
 */
public record BarcodeResponse(
    String arNumber,
    String productName,
    String batchNo,
    String storageCondition,
    String barcodeString,
    Instant generatedAt
) {
    public static BarcodeResponse of(String arNumber, String productName, String batchNo,
                                      String storageCondition, String barcodeString) {
        return new BarcodeResponse(
            arNumber,
            productName,
            batchNo,
            storageCondition,
            barcodeString,
            Instant.now()
        );
    }
}
