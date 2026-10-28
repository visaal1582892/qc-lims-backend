package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.MetricSpecificationDao;
import com.medplus.qc_lims_backend.dao.mapper.MetricSpecificationRowMapper;
import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.criteria.MetricSpecificationSearchCriteria;
import com.medplus.qc_lims_backend.util.SqlUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class MetricSpecificationDaoImpl extends AbstractMasterDao<MetricSpecification, MetricSpecificationSearchCriteria> implements MetricSpecificationDao {

    private static final Map<String, String> SORT_WHITELIST = Map.ofEntries(
            Map.entry("updatedat", "COALESCE(ms.updated_at, ms.created_at)"),
            Map.entry("createdat", "ms.created_at"),
            Map.entry("id", "ms.metric_specification_id"),
            Map.entry("configid", "ms.config_id"),
            Map.entry("productid", "ic.product_id"),
            Map.entry("metricname", "mm.metric_name"),
            Map.entry("metricunit", "mm.metric_name"),
            Map.entry("unit", "mm.unit"),
            Map.entry("lowerbound", "ms.lower_bound"),
            Map.entry("upperbound", "ms.upper_bound"),
            Map.entry("status", "ms.status")
    );

    // Singleton RowMapper instance to avoid allocation per query execution
    private final MetricSpecificationRowMapper rowMapper = new MetricSpecificationRowMapper();

    public MetricSpecificationDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getFromClauseWithJoins() {
        return "metric_specifications ms "
                + "LEFT JOIN qc_inventory_configuration ic ON ms.config_id = ic.config_id "
                + "LEFT JOIN test_parameter_master tp ON ic.param_id = tp.param_id "
                + "LEFT JOIN metric_master mm ON ms.metric_id = mm.metric_id";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "ms.metric_specification_id";
    }

    @Override
    protected String getStatusColumn() {
        return "ms.status";
    }

    @Override
    protected String getSelectColumnsWithJoins() {
        return "SELECT ms.metric_specification_id, ms.config_id, ms.metric_id, ms.lower_bound, ms.upper_bound, ms.status, "
                + "ms.created_at, ms.created_by, ms.updated_at, ms.updated_by, "
                + "ic.product_id, ic.test_method_id, tp.name AS test_parameter_name, "
                + "mm.metric_name AS metric_name, mm.unit AS metric_unit "
                + "FROM metric_specifications ms "
                + "LEFT JOIN qc_inventory_configuration ic ON ms.config_id = ic.config_id "
                + "LEFT JOIN test_parameter_master tp ON ic.param_id = tp.param_id "
                + "LEFT JOIN metric_master mm ON ms.metric_id = mm.metric_id ";
    }

    @Override
    protected RowMapper<MetricSpecification> getRowMapper() {
        return rowMapper;
    }

    @Override
    protected Map<String, String> getSortWhitelist() {
        return SORT_WHITELIST;
    }

    @Override
    protected void appendDomainFilters(MetricSpecificationSearchCriteria criteria, StringBuilder where, MapSqlParameterSource params) {
        if (criteria.getConfigId() != null) {
            where.append(" AND ms.config_id = :configId ");
            params.addValue("configId", criteria.getConfigId());
        }

        if (StringUtils.hasText(criteria.getProductId())) {
            where.append(" AND LOWER(ic.product_id) LIKE :productId ");
            params.addValue("productId", SqlUtils.escapeLike(criteria.getProductId()));
        }

        if (criteria.getMetricId() != null) {
            where.append(" AND ms.metric_id = :metricId ");
            params.addValue("metricId", criteria.getMetricId());
        }

        if (StringUtils.hasText(criteria.getMetricName())) {
            where.append(" AND LOWER(mm.metric_name) LIKE :metricName ");
            params.addValue("metricName", SqlUtils.escapeLike(criteria.getMetricName()));
        }

        if (StringUtils.hasText(criteria.getUnit())) {
            where.append(" AND LOWER(mm.unit) LIKE :unit ");
            params.addValue("unit", SqlUtils.escapeLike(criteria.getUnit()));
        }
    }

    @Override
    protected void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params) {
        where.append(" AND (LOWER(ic.product_id) LIKE :searchKey OR LOWER(mm.metric_name) LIKE :searchKey OR LOWER(mm.unit) LIKE :searchKey OR LOWER(tp.name) LIKE :searchKey) ");
        params.addValue("searchKey", SqlUtils.escapeLike(searchKey));
    }

    @Override
    public List<MetricSpecification> findActiveByConfigAndUnitCombos(Collection<String> comboKeys) {
        if (CollectionUtils.isEmpty(comboKeys)) {
            return List.of();
        }

        // comboKeys formatted as "CONFIGID:METRICID" (e.g., "15:2")
        String sql = getSelectColumnsWithJoins()
                + "WHERE ms.status = 'A' "
                + "  AND CONCAT(ms.config_id, ':', ms.metric_id) IN (:comboKeys) "
                + "ORDER BY ms.metric_specification_id ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("comboKeys", comboKeys);

        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
