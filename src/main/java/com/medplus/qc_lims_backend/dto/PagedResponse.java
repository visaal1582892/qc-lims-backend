package com.medplus.qc_lims_backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper matching marketing-automation backend.
 *
 * @param content       the items on the current page
 * @param totalElements total matching records (across all pages)
 * @param totalPages    total number of pages
 * @param page          0-based current page index
 * @param size          page size requested
 */
public record PagedResponse<T>(
        List<T> content,
        long    totalElements,
        int     totalPages,
        int     page,
        int     size
) {
    public static <T> PagedResponse<T> of(List<T> content, long total, int page, int size) {
        int pages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PagedResponse<>(content, total, pages, page, size);
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        if (page == null) {
            return new PagedResponse<>(List.of(), 0L, 0, 0, 0);
        }
        return new PagedResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
