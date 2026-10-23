package com.medplus.qc_lims_backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Right {
    private String rightId;
    private String rightName;
    private String description;
}
