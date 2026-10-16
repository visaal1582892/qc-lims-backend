package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for updating an e-TRF record.
 * All fields optional for partial update.
 * Includes trfStatus for lifecycle transitions.
 */
@Getter
@Setter
public class TrfUpdateRequest {

    private String companyName;
    private String companyAddress;
    private String manufacturingLicNo;
    private String contactPersonName;
    private String phoneMobile;

    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String emailId;

    private Integer productId;

    private String batchNumbers;
    private String customerArNo;

    @Pattern(regexp = "^(RM|In-process|API|Drug product|Cosmetics|Others)$",
             message = "Material type must be one of: RM, In-process, API, Drug product, Cosmetics, Others")
    private String materialType;

    private String otherMaterialTypeDesc;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private LocalDate retestDate;
    private Integer batchSize;
    private Integer sampleQty;
    private String packingCondition;

    @Pattern(regexp = "^(Ambient|Freezer|Refrigerator|Others)$",
             message = "Storage condition must be one of: Ambient, Freezer, Refrigerator, Others")
    private String storageCondition;

    private String otherStorageDesc;
    private Boolean isHygroscopic;
    private Boolean isLightSensitive;
    private Boolean isNonHazardous;
    private Boolean isHazardous;
    private Boolean isMsdsAttached;
    private String otherSampleTypeDesc;

    @Pattern(regexp = "^(By Person|By Courier|By Post|Others)$",
             message = "Submission type must be one of: By Person, By Courier, By Post, Others")
    private String submissionType;

    private String submissionTypeDesc;

    @Pattern(regexp = "^(Regulatory|Non-Regulatory/R&D)$",
             message = "Analysis type must be one of: Regulatory, Non-Regulatory/R&D")
    private String analysisType;

    private String regulatoryFormType;
    private List<Integer> testParameterIds;

    @Size(max = 50)
    private String pharmacopoeiaReference;

    private String stpDocumentNo;
    private String stpVersionNo;

    @Size(max = 100)
    private String grnNumber;

    private Integer manufacturerId;

    // Lifecycle status transition
    @Pattern(regexp = "^(Pending_Feasibility|Rejected|Accepted)$",
             message = "TRF status must be one of: Pending_Feasibility, Rejected, Accepted")
    private String trfStatus;
}
