package com.medplus.qc_lims_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event raised after TestExecutionService records an OOS result.
 *
 * Carries only the identifiers needed for OosIncidentService to build the
 * incident record.  By publishing this event instead of calling
 * OosIncidentService directly, we break the bidirectional dependency between
 * TestExecutionService and OosIncidentService.
 *
 * The OosIncidentService listens for this event (synchronously within the
 * same transaction via @TransactionalEventListener) so any rollback in OIS
 * cascades back to the test-result write.
 */
@Getter
public class TestResultOosEvent extends ApplicationEvent {

    private final Long testExecutionId;
    private final String arNumber;
    private final String testCode;
    private final String productName;
    private final Integer productId;
    private final String batchNo;
    private final String specificationReference;
    private final String observedValue;
    private final String unitOfMeasurement;
    private final String testMethodName;
    private final Long createdByUserId;

    /**
     * @source TestExecutionService.recordTestResult
     */
    public TestResultOosEvent(Object source,
                              Long testExecutionId,
                              String arNumber,
                              String testCode,
                              String productName,
                              Integer productId,
                              String batchNo,
                              String specificationReference,
                              String observedValue,
                              String unitOfMeasurement,
                              String testMethodName,
                              Long createdByUserId) {
        super(source);
        this.testExecutionId = testExecutionId;
        this.arNumber = arNumber;
        this.testCode = testCode;
        this.productName = productName;
        this.productId = productId;
        this.batchNo = batchNo;
        this.specificationReference = specificationReference;
        this.observedValue = observedValue;
        this.unitOfMeasurement = unitOfMeasurement;
        this.testMethodName = testMethodName;
        this.createdByUserId = createdByUserId;
    }
}
