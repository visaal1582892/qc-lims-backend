package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long         roleId;
    private String       roleCode;
    private String       roleName;
    private String       description;
    private RecordStatus status;
    @Builder.Default
    private List<Right> rights = Collections.emptyList();
}
