package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for closing or cancelling an AR.
 */
public class ArLifecycleRequest {

    /**
     * Mandatory reason for closing or cancelling the AR.
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
