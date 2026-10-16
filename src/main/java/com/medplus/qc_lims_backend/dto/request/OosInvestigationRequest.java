package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating an OOS investigation.
 */
public class OosInvestigationRequest {

    /** Summary of investigation findings */
    private String investigationSummary;

    /** Root cause analysis */
    private String rootCause;

    /** Corrective and preventive action */
    private String correctiveAction;

    public String getInvestigationSummary() { return investigationSummary; }
    public void setInvestigationSummary(String investigationSummary) { this.investigationSummary = investigationSummary; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String correctiveAction) { this.correctiveAction = correctiveAction; }
}
