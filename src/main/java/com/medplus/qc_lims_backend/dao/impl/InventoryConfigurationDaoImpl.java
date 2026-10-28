package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.InventoryConfigurationDao;
import com.medplus.qc_lims_backend.dao.mapper.InventoryConfigRowMapper;
import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.criteria.InventoryConfigSearchCriteria;
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
public class InventoryConfigurationDaoImpl extends AbstractMasterDao<InventoryConfiguration, InventoryConfigSearchCriteria> implements InventoryConfigurationDao {

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "updatedat", "COALESCE(ic.updated_at, ic.created_at)",
            "createdat", "ic.created_at",
            "id", "ic.config_id",
            "productid", "ic.product_id",
            "parameter", "tp.name",
            "testmethodid", "ic.test_method_id",
            "iscritical", "ic.is_critical",
            "resulttype", "ic.result_type",
            "countneeded", "ic.count_needed",
            "status", "ic.status"
    );

    // Singleton RowMapper instance to avoid allocation per query execution
    private final InventoryConfigRowMapper rowMapper = new InventoryConfigRowMapper();

    public InventoryConfigurationDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getFromClauseWithJoins() {
        return "qc_inventory_configuration ic LEFT JOIN test_parameter_master tp ON ic.param_id = tp.param_id";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "ic.config_id";
    }

    @Override
    protected String getStatusColumn() {
        return "ic.status";
    }

    @Override
    protected String getSelectColumnsWithJoins() {
        return "SELECT ic.config_id, ic.product_id, ic.param_id, ic.test_method_id, ic.is_critical, "
                + "ic.result_type, ic.specification_text, ic.count_needed, ic.status, "
                + "ic.created_at, ic.created_by, ic.updated_at, ic.updated_by, "
                + "tp.name AS test_parameter_name "
                + "FROM qc_inventory_configuration ic "
                + "LEFT JOIN test_parameter_master tp ON ic.param_id = tp.param_id ";
    }

    @Override
    protected RowMapper<InventoryConfiguration> getRowMapper() {
        return rowMapper;
    }

    @Override
    protected Map<String, String> getSortWhitelist() {
        return SORT_WHITELIST;
    }

    @Override
    protected void appendDomainFilters(InventoryConfigSearchCriteria criteria, StringBuilder where, MapSqlParameterSource params) {
        if (StringUtils.hasText(criteria.getProductId())) {
            where.append(" AND LOWER(ic.product_id) LIKE :productId ");
            params.addValue("productId", SqlUtils.escapeLike(criteria.getProductId()));
        }

        if (criteria.getParamId() != null) {
            where.append(" AND ic.param_id = :paramId ");
            params.addValue("paramId", criteria.getParamId());
        }

        if (StringUtils.hasText(criteria.getParameterName())) {
            where.append(" AND LOWER(tp.name) LIKE :parameterName ");
            params.addValue("parameterName", SqlUtils.escapeLike(criteria.getParameterName()));
        }

        if (StringUtils.hasText(criteria.getTestMethodId())) {
            where.append(" AND LOWER(ic.test_method_id) LIKE :testMethodId ");
            params.addValue("testMethodId", SqlUtils.escapeLike(criteria.getTestMethodId()));
        }

        if (StringUtils.hasText(criteria.getIsCritical())) {
            where.append(" AND ic.is_critical = :isCritical ");
            params.addValue("isCritical", criteria.getIsCritical().toUpperCase().trim());
        }

        if (StringUtils.hasText(criteria.getResultType())) {
            where.append(" AND ic.result_type = :resultType ");
            params.addValue("resultType", criteria.getResultType().toUpperCase().trim());
        }

        if (criteria.getCountNeeded() != null) {
            where.append(" AND ic.count_needed = :countNeeded ");
            params.addValue("countNeeded", criteria.getCountNeeded());
        }
    }

    @Override
    protected void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params) {
        where.append(" AND (LOWER(ic.product_id) LIKE :searchKey OR LOWER(tp.name) LIKE :searchKey "
                + "OR LOWER(ic.test_method_id) LIKE :searchKey OR LOWER(ic.specification_text) LIKE :searchKey) ");
        params.addValue("searchKey", SqlUtils.escapeLike(searchKey));
    }

    @Override
    public List<InventoryConfiguration> findActiveByProductAndParamCombos(Collection<String> comboKeys) {
        if (CollectionUtils.isEmpty(comboKeys)) {
            return List.of();
        }

        // comboKeys formatted as "PRODUCTID:PARAMID" (e.g., "PROD01:15")
        String sql = getSelectColumnsWithJoins()
                + "WHERE ic.status = 'A' "
                + "  AND CONCAT(UPPER(TRIM(ic.product_id)), ':', ic.param_id) IN (:comboKeys) "
                + "ORDER BY ic.config_id ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("comboKeys", comboKeys);

        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
