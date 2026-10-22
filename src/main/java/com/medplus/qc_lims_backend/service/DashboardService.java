package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.response.AnalystProductivityResponse;
import com.medplus.qc_lims_backend.dto.response.DashboardMetricsResponse;
import com.medplus.qc_lims_backend.dto.response.ManufacturerSampleResponse;
import com.medplus.qc_lims_backend.dto.response.ParameterAnalyticsResponse;
import com.medplus.qc_lims_backend.dto.response.StatusDistributionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard aggregation service interface (SRS Module 12.1).
 */
public interface DashboardService {

    /**
     * Aggregate all dashboard KPI metrics in a single call.
     */
    DashboardMetricsResponse getMetrics();

    /**
     * Get all samples associated with a given manufacturer.
     */
    List<ManufacturerSampleResponse> getManufacturerWise(Integer manufacturerId);

    /**
     * Get analyst productivity within a date range.
     */
    AnalystProductivityResponse getAnalystProductivity(Integer analystId, LocalDate startDate, LocalDate endDate);

    /**
     * Track samples by date range and/or batch number.
     */
    List<ManufacturerSampleResponse> getDateBatchTracking(LocalDate startDate, LocalDate endDate, String batchNo);

    /**
     * Get analytics for a specific test parameter.
     */
    ParameterAnalyticsResponse getParameterAnalytics(Integer parameterId);

    /**
     * Filter and count samples by lifecycle status.
     */
    List<StatusDistributionResponse> getStatusFiltering(String status);
}
