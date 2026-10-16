package com.medplus.qc_lims_backend.dto.response;

/**
 * Status distribution response.
 *
 * Returned by GET /api/dashboard/status/{status} endpoint.
 * Provides counts of samples grouped by lifecycle status.
 */
public record StatusDistributionResponse(
    String statusName,
    Long   count
) {}
