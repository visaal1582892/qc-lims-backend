package com.medplus.qc_lims_backend.dto;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;
    private String email;
    private String departmentId;
    private String departmentName;
    private String designationId;
    private RecordStatus status;
}
