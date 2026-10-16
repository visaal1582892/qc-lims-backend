package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.AnalyticsQueryRequest;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSampleResponse;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSummaryResponse;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.DashboardMetricsResponse;
import com.medplus.qc_lims_backend.dto.response.PageResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.AnalyticsService;
import com.medplus.qc_lims_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Dashboard and Analytics endpoints (SRS Module 12).
 *
 * Endpoints:
 * - GET  /api/dashboard/metrics              — Dashboard KPI metrics (R-13)
 * - GET  /api/dashboard/analytics/samples     — Multi-dimensional search (R-14)
 * - GET  /api/dashboard/analytics/summary/by-parameter — Parameter summary (R-10)
 * - GET  /api/dashboard/analytics/summary/by-analyst   — Analyst productivity (R-08)
 *
 * Security: C-4 — Uses Rights constant concatenation in @PreAuthorize.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;

    // --- Dashboard Metrics ---

    /**
     * GET /api/dashboard/metrics
     * Returns all KPI metrics as a flat response (R-13).
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_DASHBOARD_VIEW + "')")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMetrics()));
    }

    // --- Analytics: Multi-Dimensional Search ---

    /**
     * GET /api/dashboard/analytics/samples
     * Multi-dimensional sample search with filters and pagination (R-14).
     * C-7: No @Valid needed on record DTO bound from query params — Spring handles defaults.
     */
    @GetMapping("/analytics/samples")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_ANALYTICS_VIEW + "')")
    public ResponseEntity<ApiResponse<PageResponse<AnalyticsSampleResponse>>> searchSamples(
            AnalyticsQueryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.searchSamples(request)));
    }

    /**
     * GET /api/dashboard/analytics/summary/by-parameter
     * Parameter-wise aggregated analytics summary (R-10).
     */
    @GetMapping("/analytics/summary/by-parameter")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_ANALYTICS_VIEW + "')")
    public ResponseEntity<ApiResponse<List<AnalyticsSummaryResponse>>> summaryByParameter() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.summaryByParameter()));
    }

    /**
     * GET /api/dashboard/analytics/summary/by-analyst
     * Analyst productivity summary for a date range (R-08).
     */
    @GetMapping("/analytics/summary/by-analyst")
    @PreAuthorize("hasAuthority('" + Rights.ROLE_ANALYTICS_VIEW + "')")
    public ResponseEntity<ApiResponse<List<AnalyticsSummaryResponse>>> summaryByAnalyst(
            LocalDate from,
            LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.summaryByAnalyst(from, to)));
    }
}
