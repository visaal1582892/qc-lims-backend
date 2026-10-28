package com.medplus.qc_lims_backend.dto.external;

import java.util.Set;

public record ProductRequestDto(
        Set<String> productIdList,
        String productStatus
) {}
