package com.medplus.qc_lims_backend.dto.response;

/**
 * Manufacturer-wise sample detail response.
 *
 * Returned by GET /api/dashboard/manufacturer/{id} endpoint.
 * Contains aggregated sample counts per manufacturer, along with
 * a list of associated sample records.
 */
public record ManufacturerSampleResponse(
    Long   inwardId,
    String arNumber,
    String batchNo,
    String arStatus,
    String trfStatus,
    String productName,
    String manufacturerName
) {}
