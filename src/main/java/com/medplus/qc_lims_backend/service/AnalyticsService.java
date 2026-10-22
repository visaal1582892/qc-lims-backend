package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.AnalyticsQueryRequest;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSampleResponse;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSummaryResponse;
import com.medplus.qc_lims_backend.dto.response.PageResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Multi-dimensional analytics service interface (SRS Module 12.2).
 */
public interface AnalyticsService {

    /**
     * Search samples with multi-dimensional filters and pagination (R-12).
     */
    PageResponse<AnalyticsSampleResponse> searchSamples(AnalyticsQueryRequest request);

    /**
     * Parameter-wise analytics summary (R-10).
     */
    List<AnalyticsSummaryResponse> summaryByParameter();

    /**
     * Analyst productivity summary for a date range (R-08).
     */
    List<AnalyticsSummaryResponse> summaryByAnalyst(LocalDate from, LocalDate to);
}
