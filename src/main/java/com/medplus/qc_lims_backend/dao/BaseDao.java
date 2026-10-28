package com.medplus.qc_lims_backend.dao;

import com.medplus.qc_lims_backend.domain.criteria.BaseSearchCriteria;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.Collection;
import java.util.List;

/**
 * Common data access interface for master entities.
 * Strictly separates read queries (JdbcTemplate) from mutations and provides
 * unified support for 3-query deferred join pagination, bounded criteria lookups,
 * and high-volume streaming exports.
 *
 * @param <T> Domain entity type
 * @param <C> Search criteria type extending BaseSearchCriteria
 */
public interface BaseDao<T, C extends BaseSearchCriteria> {

    /**
     * Total count of matching records for pagination (Query 1 of deferred join).
     */
    long count(C criteria);

    /**
     * Database-level paginated ID retrieval (Query 2 of deferred join).
     */
    List<Long> getPageIds(C criteria, int offset, int limit);

    /**
     * Batch fetch full entities by IDs with exact order preservation (Query 3 of deferred join).
     */
    List<T> getEntitiesByIds(List<Long> ids);

    /**
     * Bounded criteria query for dropdowns, lookups, and downstream services.
     * Enforces a hard SQL limit (capped at MAX_QUERY_LIMIT) to prevent JVM heap exhaustion.
     */
    List<T> findList(C criteria, int maxResults);

    /**
     * Bounded batch lookup for active entities matching specific IDs.
     */
    List<T> findActiveByIds(Collection<Long> ids);

    /**
     * Streams query results row-by-row via RowCallbackHandler for large Excel/CSV exports
     * without accumulating entity objects in the JVM heap.
     */
    void stream(C criteria, RowCallbackHandler handler);
}
