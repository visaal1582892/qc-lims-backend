package com.medplus.qc_lims_backend.dto.response;

/**
 * Analyst productivity response.
 *
 * Returned by GET /api/dashboard/analyst/{id}/productivity endpoint.
 * Contains test counts broken down by compliance outcome for an analyst
 * within a date range.
 */
public record AnalystProductivityResponse(
    Integer analystId,
    Long    totalTests,
    Long    completed,
    Long    passed,
    Long    failed,
    Long    pending,
    Long    onHold
) {}
