package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.TOSTriggerRequest;
import com.medplus.qc_lims_backend.dto.request.TOSUpdateRequest;
import com.medplus.qc_lims_backend.dto.response.TOSTriggerResponse;
import com.medplus.qc_lims_backend.dto.response.TOSUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service layer for external TOS (Test Order Specification) hooks.
 *
 * Per SRS Module 14.1, these are placeholder stubs — all payloads are
 * logged for audit trail but no business logic (e-TRF auto-creation)
 * is implemented yet. That logic is reserved for future work.
 *
 * PLACEHOLDER: Actual TOS/e-TRF generation logic to be implemented later
 * per SRS Executive Overview §2. When implemented, this service will
 * delegate to TrfService.create() to auto-create TRF records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExternalTosService {

    /**
     * Process an incoming TOS trigger payload.
     *
     * Currently logs the payload and returns an acknowledgment.
     * Future implementation will auto-create an e-TRF via TrfService.
     *
     * @param request the incoming GRN/Transfer Order payload
     * @return acknowledgment with accepted=true and placeholder message
     */
    public TOSTriggerResponse processTosTrigger(TOSTriggerRequest request) {
        log.info("EXTERNAL TOS TRIGGER RECEIVED: grn={}, transferOrder={}, manufacturer={}, product={}, batches={}",
                request.grnNumber(),
                request.transferOrderNumber(),
                request.manufacturerId(),
                request.productId(),
                request.batchNumbers());

        if (request.requestedTestParameters() != null && !request.requestedTestParameters().isEmpty()) {
            log.info("  Requested test parameters: {}", request.requestedTestParameters());
        }

        if (request.remarks() != null && !request.remarks().isBlank()) {
            log.info("  Remarks: {}", request.remarks());
        }

        return new TOSTriggerResponse(
                true,
                null, // trfId will be set when actual e-TRF creation is implemented
                "TOS trigger received and logged. e-TRF auto-creation pending.",
                Instant.now()
        );
    }

    /**
     * Process an incoming TOS status update callback.
     *
     * Currently logs the payload and returns an acknowledgment.
     * Future implementation will update existing TRF records accordingly.
     *
     * @param request the incoming status update payload
     * @return acknowledgment with accepted=true and placeholder message
     */
    public TOSUpdateResponse processTosUpdate(TOSUpdateRequest request) {
        String identifier = request.grnNumber() != null && !request.grnNumber().isBlank()
                ? "grn=" + request.grnNumber()
                : "trfId=" + request.trfId();

        log.info("EXTERNAL TOS UPDATE RECEIVED: identifier={}, status={}", identifier, request.status());

        if (request.updatedFields() != null && !request.updatedFields().isEmpty()) {
            log.info("  Updated fields: {}", request.updatedFields());
        }

        if (request.remarks() != null && !request.remarks().isBlank()) {
            log.info("  Remarks: {}", request.remarks());
        }

        return new TOSUpdateResponse(
                true,
                "TOS update received and logged. Record update pending.",
                Instant.now()
        );
    }
}
