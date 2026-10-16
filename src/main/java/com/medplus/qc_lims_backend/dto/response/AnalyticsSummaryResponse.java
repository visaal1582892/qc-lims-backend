package com.medplus.qc_lims_backend.dto.response;

/**
 * Analytics summary response DTO for parameter-wise aggregated analytics.
 *
 * Used by GET /api/dashboard/analytics/summary/by-parameter endpoint.
 * Provides per-parameter breakdown: total tests, passed, failed, pending.
 */
public record AnalyticsSummaryResponse(
    Long   parameterId,
    String  parameterName,
    Long   totalTests,
    Long   passed,
    Long   failed,
    Long   pending
) {}
