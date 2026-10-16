package com.medplus.qc_lims_backend.dto.response;

/**
 * Parameter analytics detail response.
 *
 * Returned by GET /api/dashboard/parameters/{id}/analytics endpoint.
 * Provides a breakdown of test execution results for a specific parameter.
 */
public record ParameterAnalyticsResponse(
    Long   parameterId,
    String parameterName,
    Long   totalTests,
    Long   passed,
    Long   failed,
    Long   pending,
    Long   hold
) {}
