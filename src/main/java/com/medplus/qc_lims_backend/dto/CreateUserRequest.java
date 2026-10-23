package com.medplus.qc_lims_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank
    private String employeeId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String password;

    private List<Long> roleIds;
}
