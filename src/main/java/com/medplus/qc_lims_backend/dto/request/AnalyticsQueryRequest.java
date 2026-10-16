package com.medplus.qc_lims_backend.dto.request;

import java.time.LocalDate;

/**
 * Analytics query parameters DTO (R-15).
 *
 * Supports all multi-dimensional filter fields from SRS Module 12.2:
 * manufacturerId, analystId, dateFrom/dateTo, batchNumber, parameterId,
 * executionStatus, plus pagination (page/size) and sorting (sortBy/sortDirection).
 *
 * Defaults enforced in compact constructor:
 * - page: 0 (minimum)
 * - size: 20 (default, max 100)
 * - sortDirection: ASC (default)
 */
public record AnalyticsQueryRequest(
    Integer  manufacturerId,    // R-07: Manufacturer-wise search
    Integer  analystId,         // R-08: Analyst-wise productivity
    LocalDate dateFrom,         // R-09: Date-range filtering
    LocalDate dateTo,           // R-09
    String   batchNumber,       // R-09: Batch/lot number search
    Integer  parameterId,       // R-10: Parameter-wise analytics
    String   executionStatus,   // R-11: Status-based filtering
    Integer  page,              // R-12: Pagination
    Integer  size,              // R-12
    String   sortBy,            // R-12: Sort field
    String   sortDirection      // R-12: Sort direction
) {
    public AnalyticsQueryRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1 || size > 100) size = 20;
        if (sortDirection == null || (!"ASC".equals(sortDirection) && !"DESC".equals(sortDirection)))
            sortDirection = "ASC";
    }
}
