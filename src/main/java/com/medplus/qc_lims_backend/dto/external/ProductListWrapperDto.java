package com.medplus.qc_lims_backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductListWrapperDto(
        List<ProductItemDto> productList,
        Integer totalRows
) {}
