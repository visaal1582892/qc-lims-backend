package com.medplus.qc_lims_backend.dto.response;

import java.util.Date;

/**
 * Analytics sample response DTO.
 *
 * Projection DTO for individual sample records returned by the
 * multi-dimensional analytics search endpoint (R-14).
 *
 * Joins sample_inward_register with test_execution to provide
 * execution-level details alongside sample metadata.
 */
public record AnalyticsSampleResponse(
    Long   inwardId,
    String arNumber,
    Integer manufacturerId,
    String  batchNo,
    String  arStatus,
    String  executionStatus,
    String  complianceFlag,
    Long   parameterId,
    String  parameterName,
    Integer analystId,
    Date   receiptDate,
    Date   submittedAt
) {}
