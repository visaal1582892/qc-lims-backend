package com.medplus.qc_lims_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event raised when a batch is rejected after 3 consecutive OOS failures.
 *
 * Carries the identifiers needed for notification (email/webhook stub).
 * Listened by OosNotificationListener (logging-only stub — no spring-boot-starter-mail).
 */
@Getter
public class OosBatchRejectedEvent extends ApplicationEvent {

    private final Long incidentId;
    private final String incidentNumber;
    private final String arNumber;
    private final String productName;
    private final String batchNo;
    private final Integer productId;
    private final String reason;

    public OosBatchRejectedEvent(Object source,
                                 Long incidentId,
                                 String incidentNumber,
                                 String arNumber,
                                 String productName,
                                 String batchNo,
                                 Integer productId,
                                 String reason) {
        super(source);
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.arNumber = arNumber;
        this.productName = productName;
        this.batchNo = batchNo;
        this.productId = productId;
        this.reason = reason;
    }
}
