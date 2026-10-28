package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.BaseDao;
import com.medplus.qc_lims_backend.domain.criteria.BaseSearchCriteria;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstract base DAO implementing the Template Method Pattern for master data access.
 * Centralizes count, paginated ID retrieval, order-preserved batch hydration,
 * bounded criteria lookups, and memory-safe streaming exports.
 *
 * @param <T> Domain entity type
 * @param <C> Criteria type extending BaseSearchCriteria
 */
public abstract class AbstractMasterDao<T, C extends BaseSearchCriteria> implements BaseDao<T, C> {

    /**
     * Hard safety ceiling for bounded criteria queries to protect the JVM heap against OOM.
     */
    protected static final int MAX_QUERY_LIMIT = 2000;

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected AbstractMasterDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Template Hooks (To be implemented by concrete child DAOs) ──

    /**
     * The table expression with any required joins used in COUNT and ID queries
     * (e.g. "metric_unit_master m" or "test_parameter_master tp LEFT JOIN metric_unit_master mu ON ...").
     */
    protected abstract String getFromClauseWithJoins();

    /**
     * The qualified primary key column expression (e.g. "m.metric_unit_id", "tp.param_id").
     */
    protected abstract String getPrimaryKeyColumn();

    /**
     * The qualified status column expression (e.g. "m.status", "tp.status").
     */
    protected abstract String getStatusColumn();

    /**
     * The full SELECT ... FROM ... [JOINS] query for detail hydration.
     */
    protected abstract String getSelectColumnsWithJoins();

    /**
     * Returns the singleton RowMapper instance for mapping ResultSets to domain entities.
     * Child DAOs must return a private final singleton rather than creating a new instance per query.
     */
    protected abstract RowMapper<T> getRowMapper();

    /**
     * Returns the sort whitelist map mapping lowercase sort fields to SQL column expressions.
     */
    protected abstract Map<String, String> getSortWhitelist();

    /**
     * Appends entity-specific WHERE conditions and binds parameters.
     */
    protected abstract void appendDomainFilters(C criteria, StringBuilder where, MapSqlParameterSource params);

    /**
     * Appends entity-specific multi-column LIKE search condition using the escaped keyword.
     */
    protected abstract void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params);

    // ── Universal Query Implementations ──────────────────────────

    @Override
    public long count(C criteria) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT COUNT(" + getPrimaryKeyColumn() + ") FROM " + getFromClauseWithJoins()
                + buildWhereClause(criteria, params);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<Long> getPageIds(C criteria, int offset, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT " + getPrimaryKeyColumn() + " FROM " + getFromClauseWithJoins()
                + buildWhereClause(criteria, params)
                + buildOrderByClause(criteria)
                + " LIMIT :limit OFFSET :offset";

        params.addValue("limit", Math.max(1, limit));
        params.addValue("offset", Math.max(0, offset));

        return jdbcTemplate.queryForList(sql, params, Long.class);
    }

    @Override
    public List<T> getEntitiesByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        String sql = getSelectColumnsWithJoins()
                + " WHERE " + getPrimaryKeyColumn() + " IN (:ids) "
                + " ORDER BY FIELD(" + getPrimaryKeyColumn() + ", :ids)";

        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    @Override
    public List<T> findList(C criteria, int maxResults) {
        int safeLimit = Math.min(Math.max(1, maxResults), MAX_QUERY_LIMIT);
        List<Long> ids = getPageIds(criteria, 0, safeLimit);
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return getEntitiesByIds(ids);
    }

    @Override
    public List<T> findActiveByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        String sql = getSelectColumnsWithJoins()
                + " WHERE " + getPrimaryKeyColumn() + " IN (:ids) AND " + getStatusColumn() + " = 'A' "
                + " ORDER BY " + getPrimaryKeyColumn() + " ASC";

        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    @Override
    public void stream(C criteria, RowCallbackHandler handler) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = getSelectColumnsWithJoins()
                + buildWhereClause(criteria, params)
                + buildOrderByClause(criteria);

        jdbcTemplate.query(sql, params, handler);
    }

    // ── Common SQL Clause Builders ───────────────────────────────

    protected String buildWhereClause(C criteria, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");

        if (criteria == null) {
            return where.toString();
        }

        if (criteria.getId() != null) {
            where.append(" AND ").append(getPrimaryKeyColumn()).append(" = :baseId ");
            params.addValue("baseId", criteria.getId());
        }

        if (!CollectionUtils.isEmpty(criteria.getIdList())) {
            where.append(" AND ").append(getPrimaryKeyColumn()).append(" IN (:baseIdList) ");
            params.addValue("baseIdList", criteria.getIdList());
        }

        if (StringUtils.hasText(criteria.getStatus()) && !"all".equalsIgnoreCase(criteria.getStatus())) {
            String s = "active".equalsIgnoreCase(criteria.getStatus()) || "a".equalsIgnoreCase(criteria.getStatus()) ? "A" : "I";
            where.append(" AND ").append(getStatusColumn()).append(" = :baseStatus ");
            params.addValue("baseStatus", s);
        } else if (!criteria.isIncludeInactive()) {
            where.append(" AND ").append(getStatusColumn()).append(" = 'A' ");
        }

        if (StringUtils.hasText(criteria.getSearchKey())) {
            appendSearchKeyFilter(criteria.getSearchKey(), where, params);
        }

        appendDomainFilters(criteria, where, params);

        return where.toString();
    }

    protected String buildOrderByClause(BaseSearchCriteria criteria) {
        String field = criteria != null && criteria.getSortField() != null
                ? criteria.getSortField().toLowerCase().trim()
                : "updatedat";

        String defaultColumn = getSortWhitelist().getOrDefault("updatedat", "updated_at");
        String column = getSortWhitelist().getOrDefault(field, defaultColumn);
        String dir = criteria != null && "ASC".equalsIgnoreCase(criteria.getSortDirection()) ? "ASC" : "DESC";

        return " ORDER BY " + column + " " + dir + ", " + getPrimaryKeyColumn() + " " + dir;
    }
}
