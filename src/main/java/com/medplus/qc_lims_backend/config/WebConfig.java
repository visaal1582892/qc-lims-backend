package com.medplus.qc_lims_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Global web configuration.
 * <p>
 * Enables stable DTO serialization for {@link org.springframework.data.domain.Page}
 * responses so Spring serializes them as a {@link org.springframework.data.rest.webmvc.PagedModel}
 * instead of the internal {@code PageImpl} class (which is not a stable public contract).
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}
