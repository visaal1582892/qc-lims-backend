package com.medplus.qc_lims_backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for OOS batch rejection notifications.
 *
 * STUB: Logs notification details only — does NOT send actual emails.
 * No spring-boot-starter-mail dependency (per plan constraint #5).
 *
 * When real email support is added later, replace log statements with
 * JavaMailSender / MailService call.
 */
@Slf4j
@Component
public class OosNotificationListener {

    /**
     * Handle batch rejection event — log notification details.
     * Runs asynchronously so it does not block the OOS resolution transaction.
     */
    @Async
    @EventListener
    public void onBatchRejected(OosBatchRejectedEvent event) {
        log.warn("[OOS-BATCH-REJECTED] incidentNumber={}, arNumber={}, product={}, batch={}, reason={}",
                event.getIncidentNumber(),
                event.getArNumber(),
                event.getProductName(),
                event.getBatchNo(),
                event.getReason());
        log.info("[OOS-BATCH-REJECTED] Notification would be sent to BD/Operations for batch recall or vendor claim.");
    }
}
