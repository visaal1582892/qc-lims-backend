package com.medplus.qc_lims_backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponseEnvelopeDto(
        String responseStatus,
        Map<String, Object> errors,
        String response,
        boolean error
) {}
