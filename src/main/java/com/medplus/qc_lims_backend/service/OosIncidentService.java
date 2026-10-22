package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.AuthorizeRetestRequest;
import com.medplus.qc_lims_backend.dto.request.CompleteRetestRequest;
import com.medplus.qc_lims_backend.dto.request.OosInvestigationRequest;
import com.medplus.qc_lims_backend.dto.request.RaiseOosIncidentRequest;
import com.medplus.qc_lims_backend.dto.request.RejectBatchRequest;
import com.medplus.qc_lims_backend.dto.response.OosIncidentResponse;
import com.medplus.qc_lims_backend.event.TestResultOosEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for OOS Incident Management (SRS Module 8).
 */
public interface OosIncidentService {

    /**
     * Listen for OOS events published by TestExecutionService.
     */
    void handleOosEvent(TestResultOosEvent event);

    /**
     * OIS-01: Authorize Retest (QC Head)
     */
    OosIncidentResponse authorizeRetest(Long incidentId, AuthorizeRetestRequest request);

    /**
     * OIS-02: Complete Retest
     */
    OosIncidentResponse completeRetest(Long incidentId, CompleteRetestRequest request);

    /**
     * OIS-03: Reject Batch
     */
    OosIncidentResponse rejectBatch(Long incidentId, RejectBatchRequest request);

    /**
     * OIS-04: List OOS Incidents (paginated, with filters)
     */
    Page<OosIncidentResponse> listIncidents(String arNumber, String status,
                                             String disposition, Pageable pageable);

    /**
     * OIS-05: Get incident detail with retest chain
     */
    OosIncidentResponse getIncident(Long incidentId);

    /**
     * OIS-06: List incidents by AR number
     */
    List<OosIncidentResponse> getIncidentsByAr(String arNumber);

    /**
     * OIS-07: Manually raise an OOS incident
     */
    OosIncidentResponse raiseOosIncident(RaiseOosIncidentRequest request);

    /**
     * OIS-08: Submit investigation findings
     */
    OosIncidentResponse investigate(Long incidentId, OosInvestigationRequest request);
}
