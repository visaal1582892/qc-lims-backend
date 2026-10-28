package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.MetricDao;
import com.medplus.qc_lims_backend.dao.mapper.MetricRowMapper;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.criteria.MetricSearchCriteria;
import com.medplus.qc_lims_backend.util.SqlUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Map;

@Repository
public class MetricDaoImpl extends AbstractMasterDao<Metric, MetricSearchCriteria> implements MetricDao {

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "updatedat", "COALESCE(m.updated_at, m.created_at)",
            "createdat", "m.created_at",
            "id", "m.metric_id",
            "metricname", "m.metric_name",
            "name", "m.metric_name",
            "unit", "m.unit",
            "subname", "m.unit",
            "status", "m.status"
    );

    // Singleton RowMapper instance to avoid allocation per query execution
    private final MetricRowMapper rowMapper = new MetricRowMapper();

    public MetricDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getFromClauseWithJoins() {
        return "metric_master m";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "m.metric_id";
    }

    @Override
    protected String getStatusColumn() {
        return "m.status";
    }

    @Override
    protected String getSelectColumnsWithJoins() {
        return "SELECT m.metric_id, m.metric_name, m.unit, m.status, "
                + "m.created_at, m.created_by, m.updated_at, m.updated_by "
                + "FROM metric_master m ";
    }

    @Override
    protected RowMapper<Metric> getRowMapper() {
        return rowMapper;
    }

    @Override
    protected Map<String, String> getSortWhitelist() {
        return SORT_WHITELIST;
    }

    @Override
    protected void appendDomainFilters(MetricSearchCriteria criteria, StringBuilder where, MapSqlParameterSource params) {
        if (StringUtils.hasText(criteria.getMetricName())) {
            where.append(" AND LOWER(m.metric_name) LIKE :metricName ");
            params.addValue("metricName", SqlUtils.escapeLike(criteria.getMetricName()));
        }

        if (StringUtils.hasText(criteria.getUnit())) {
            where.append(" AND LOWER(m.unit) LIKE :unit ");
            params.addValue("unit", SqlUtils.escapeLike(criteria.getUnit()));
        }
    }

    @Override
    protected void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params) {
        where.append(" AND (LOWER(m.metric_name) LIKE :searchKey OR LOWER(m.unit) LIKE :searchKey) ");
        params.addValue("searchKey", SqlUtils.escapeLike(searchKey));
    }
}
