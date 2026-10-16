package com.medplus.qc_lims_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary response for OOS incident list view.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OosIncidentSummaryResponse(
        Long id,
        String incidentNumber,
        String arNumber,
        String productName,
        String batchNo,
        String testCode,
        String oosStatus,
        String finalDisposition,
        Integer retestAttemptCount,
        String createdAt
) {
}
