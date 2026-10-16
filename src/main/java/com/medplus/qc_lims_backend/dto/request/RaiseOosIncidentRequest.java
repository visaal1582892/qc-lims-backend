package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for manually raising an OOS incident.
 */
public class RaiseOosIncidentRequest {

    @NotBlank(message = "AR number is required")
    private String arNumber;

    @NotNull(message = "Execution ID is required")
    private Integer executionId;

    private Integer productId;
    private String productName;
    private String batchNo;
    private String testCode;
    private String specificationReference;
    private String observedValue;
    private String unitOfMeasurement;
    private String deviationDescription;
    private String testMethodName;

    public String getArNumber() { return arNumber; }
    public void setArNumber(String arNumber) { this.arNumber = arNumber; }
    public Integer getExecutionId() { return executionId; }
    public void setExecutionId(Integer executionId) { this.executionId = executionId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
    public String getSpecificationReference() { return specificationReference; }
    public void setSpecificationReference(String specificationReference) { this.specificationReference = specificationReference; }
    public String getObservedValue() { return observedValue; }
    public void setObservedValue(String observedValue) { this.observedValue = observedValue; }
    public String getUnitOfMeasurement() { return unitOfMeasurement; }
    public void setUnitOfMeasurement(String unitOfMeasurement) { this.unitOfMeasurement = unitOfMeasurement; }
    public String getDeviationDescription() { return deviationDescription; }
    public void setDeviationDescription(String deviationDescription) { this.deviationDescription = deviationDescription; }
    public String getTestMethodName() { return testMethodName; }
    public void setTestMethodName(String testMethodName) { this.testMethodName = testMethodName; }
}
