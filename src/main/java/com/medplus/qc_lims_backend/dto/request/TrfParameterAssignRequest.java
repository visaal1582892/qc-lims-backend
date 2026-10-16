package com.medplus.qc_lims_backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for assigning test parameters to an e-TRF.
 */
@Getter
@Setter
public class TrfParameterAssignRequest {

    @NotNull(message = "TRF ID is required")
    private Integer trfId;

    @NotEmpty(message = "At least one parameter ID is required")
    private List<Integer> parameterIds;
}
