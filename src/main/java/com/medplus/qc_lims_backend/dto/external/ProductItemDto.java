package com.medplus.qc_lims_backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductItemDto(
        String productId,
        String name,
        String status
) {}
