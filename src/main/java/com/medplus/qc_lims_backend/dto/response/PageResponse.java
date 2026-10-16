package com.medplus.qc_lims_backend.dto.response;

import java.util.List;

/**
 * Pagination envelope per architecture Pattern B.
 */
public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
