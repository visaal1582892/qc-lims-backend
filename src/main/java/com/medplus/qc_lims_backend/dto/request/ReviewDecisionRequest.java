package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for QC/QA Head review decisions.
 *
 * Used for both QC Head and QA Head review endpoints.
 * Decision is APPROVE or REWORK.
 * Reason is mandatory when decision is REWORK.
 */
public class ReviewDecisionRequest {

    @NotBlank(message = "Decision is required (APPROVE or REWORK)")
    private String decision;

    private String reason;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
