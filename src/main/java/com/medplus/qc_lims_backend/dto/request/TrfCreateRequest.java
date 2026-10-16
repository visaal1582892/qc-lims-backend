package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating an e-TRF record.
 * Covers all SRS 3.1 A-D fields.
 */
@Getter
@Setter
public class TrfCreateRequest {

    // Section A — Customer/Manufacturer Details
    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    private String companyName;

    private String companyAddress;

    @NotBlank(message = "Manufacturing license number is required")
    @Size(max = 100)
    private String manufacturingLicNo;

    @Size(max = 150)
    private String contactPersonName;

    @Size(max = 50)
    private String phoneMobile;

    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String emailId;

    // Section B — Sample Particulars
    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotBlank(message = "Batch numbers are required")
    private String batchNumbers;

    @Size(max = 100)
    private String customerArNo;

    @Pattern(regexp = "^(RM|In-process|API|Drug product|Cosmetics|Others)$",
             message = "Material type must be one of: RM, In-process, API, Drug product, Cosmetics, Others")
    private String materialType;

    @Size(max = 150)
    private String otherMaterialTypeDesc;

    @NotNull(message = "Manufacturing date is required")
    private LocalDate mfgDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expDate;

    @NotNull(message = "Retest date is required")
    private LocalDate retestDate;

    private Integer batchSize;

    @NotNull(message = "Sample quantity is required")
    private Integer sampleQty;

    @Size(max = 150)
    private String packingCondition;

    @Pattern(regexp = "^(Ambient|Freezer|Refrigerator|Others)$",
             message = "Storage condition must be one of: Ambient, Freezer, Refrigerator, Others")
    private String storageCondition;

    @Size(max = 150)
    private String otherStorageDesc;

    private Boolean isHygroscopic;
    private Boolean isLightSensitive;
    private Boolean isNonHazardous;
    private Boolean isHazardous;
    private Boolean isMsdsAttached;

    @Size(max = 150)
    private String otherSampleTypeDesc;

    @Pattern(regexp = "^(By Person|By Courier|By Post|Others)$",
             message = "Submission type must be one of: By Person, By Courier, By Post, Others")
    private String submissionType;

    @Size(max = 150)
    private String submissionTypeDesc;

    // Section C — Analysis Requirements
    @NotBlank(message = "Analysis type is required")
    @Pattern(regexp = "^(Regulatory|Non-Regulatory/R&D)$",
             message = "Analysis type must be one of: Regulatory, Non-Regulatory/R&D")
    private String analysisType;

    @Size(max = 100)
    private String regulatoryFormType;

    @NotEmpty(message = "At least one test parameter must be assigned")
    private List<Integer> testParameterIds;

    @NotBlank(message = "Pharmacopoeia reference is required")
    @Size(max = 50)
    @Pattern(regexp = "^(USP|BP|EP|IP|JP|IS|MOA|STP)$",
             message = "Pharmacopoeia reference must be one of: USP, BP, EP, IP, JP, IS, MOA, STP")
    private String pharmacopoeiaReference;

    @Size(max = 100)
    private String stpDocumentNo;

    @Size(max = 50)
    private String stpVersionNo;

    // Section D
    @Size(max = 100)
    private String grnNumber;

    @NotNull(message = "Manufacturer ID is required")
    private Integer manufacturerId;

    @NotNull(message = "Submitted by user ID is required")
    private Integer submittedByUserId;
}
