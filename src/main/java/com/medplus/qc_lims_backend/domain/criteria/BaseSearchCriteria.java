package com.medplus.qc_lims_backend.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Base search criteria carrying common pagination, sorting, and filter fields.
 * Follows database-search-patterns-guide.md §3.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseSearchCriteria {

    private Long id;
    private List<Long> idList;
    private String searchKey;
    private String status;

    @Builder.Default
    private boolean includeInactive = false;

    // Pagination (0-based)
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    // Dynamic Sorting (default: latest modified/added to oldest)
    @Builder.Default
    private String sortField = "updatedAt";

    @Builder.Default
    private String sortDirection = "DESC";

    public int getOffset() {
        return Math.max(0, page) * Math.max(1, size);
    }
}
