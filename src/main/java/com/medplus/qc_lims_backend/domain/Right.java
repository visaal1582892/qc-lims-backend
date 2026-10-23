package com.medplus.qc_lims_backend.domain;

import com.medplus.qc_lims_backend.enums.RecordStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Right {
    private String       rightId;
    private String       rightName;
    private String       description;
    private RecordStatus status;
}
