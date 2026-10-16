package com.medplus.qc_lims_backend.dto.response;

/**
 * Dashboard metrics response DTO (R-06).
 *
 * Flat structure for single-endpoint dashboard KPI response.
 * All counts are non-null — defaults to 0L when no data exists.
 *
 * Fields mapped to SRS Module 12.1:
 * - Inward Requests: pendingFeasibility, accepted, rejected
 * - Active Execution: testing, onHold, underReview
 * - Quality Outcomes: released, qualityRejected, qualityOnHold
 * - Active OOS & Rework: activeOosIncidents, activeReworkCases
 */
public record DashboardMetricsResponse(
    // Inward Requests (R-01) — from trf_master.trf_status
    Long pendingFeasibility,
    Long accepted,
    Long rejected,

    // Active Execution Metrics (R-02) — from test_execution.execution_status
    Long testing,
    Long onHold,
    Long underReview,

    // Quality Outcomes (R-03) — from test_execution
    Long released,
    Long qualityRejected,
    Long qualityOnHold,

    // Active OOS & Rework (R-04, R-05)
    Long activeOosIncidents,
    Long activeReworkCases
) {}
