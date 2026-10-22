package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.TOSTriggerRequest;
import com.medplus.qc_lims_backend.dto.request.TOSUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TOSTriggerResponse;
import com.medplus.qc_lims_backend.dto.response.TOSUpdateResponse;

/**
 * Service layer interface for external TOS (Test Order Specification) hooks.
 *
 * Per SRS Module 14.1, these are placeholder stubs — all payloads are
 * logged for audit trail but no business logic (e-TRF auto-creation)
 * is implemented yet.
 */
public interface ExternalTosService {

    /**
     * Process an incoming TOS trigger payload.
     *
     * Currently logs the payload and returns an acknowledgment.
     * Future implementation will auto-create an e-TRF via TrfService.
     */
    TOSTriggerResponse processTosTrigger(TOSTriggerRequest request);

    /**
     * Process an incoming TOS status update callback.
     *
     * Currently logs the payload and returns an acknowledgment.
     * Future implementation will update existing TRF records accordingly.
     */
    TOSUpdateResponse processTosUpdate(TOSUpdateRequest request);
}
