package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.AnalystQualificationDao;
import com.medplus.qc_lims_backend.dao.mapper.AnalystQualificationRowMapper;
import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.domain.criteria.AnalystQualificationSearchCriteria;
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
public class AnalystQualificationDaoImpl extends AbstractMasterDao<AnalystQualification, AnalystQualificationSearchCriteria> implements AnalystQualificationDao {

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "updatedat", "COALESCE(aq.updated_at, aq.created_at)",
            "createdat", "aq.created_at",
            "id", "aq.qualification_id",
            "employeeid", "aq.employee_id",
            "parameter", "tp.name",
            "qualifiedon", "aq.qualified_on",
            "status", "aq.status"
    );

    // Singleton RowMapper instance to avoid allocation per query execution
    private final AnalystQualificationRowMapper rowMapper = new AnalystQualificationRowMapper();

    public AnalystQualificationDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getFromClauseWithJoins() {
        return "analyst_qualification_master aq LEFT JOIN test_parameter_master tp ON aq.param_id = tp.param_id";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "aq.qualification_id";
    }

    @Override
    protected String getStatusColumn() {
        return "aq.status";
    }

    @Override
    protected String getSelectColumnsWithJoins() {
        return "SELECT aq.qualification_id, aq.employee_id, aq.param_id, aq.qualified_on, aq.status, "
                + "aq.created_at, aq.created_by, aq.updated_at, aq.updated_by, "
                + "tp.name AS test_parameter_name "
                + "FROM analyst_qualification_master aq "
                + "LEFT JOIN test_parameter_master tp ON aq.param_id = tp.param_id ";
    }

    @Override
    protected RowMapper<AnalystQualification> getRowMapper() {
        return rowMapper;
    }

    @Override
    protected Map<String, String> getSortWhitelist() {
        return SORT_WHITELIST;
    }

    @Override
    protected void appendDomainFilters(AnalystQualificationSearchCriteria criteria, StringBuilder where, MapSqlParameterSource params) {
        if (StringUtils.hasText(criteria.getEmployeeId())) {
            where.append(" AND LOWER(aq.employee_id) LIKE :employeeId ");
            params.addValue("employeeId", SqlUtils.escapeLike(criteria.getEmployeeId()));
        }

        if (criteria.getParamId() != null) {
            where.append(" AND aq.param_id = :paramId ");
            params.addValue("paramId", criteria.getParamId());
        }

        if (StringUtils.hasText(criteria.getParameterName())) {
            where.append(" AND LOWER(tp.name) LIKE :parameterName ");
            params.addValue("parameterName", SqlUtils.escapeLike(criteria.getParameterName()));
        }

        if (criteria.getQualifiedOn() != null) {
            where.append(" AND aq.qualified_on = :qualifiedOn ");
            params.addValue("qualifiedOn", java.sql.Date.valueOf(criteria.getQualifiedOn()));
        }
    }

    @Override
    protected void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params) {
        where.append(" AND (LOWER(aq.employee_id) LIKE :searchKey OR LOWER(tp.name) LIKE :searchKey) ");
        params.addValue("searchKey", SqlUtils.escapeLike(searchKey));
    }

    @Override
    public List<AnalystQualification> findActiveByEmployeeAndParamCombos(Collection<String> comboKeys) {
        if (CollectionUtils.isEmpty(comboKeys)) {
            return List.of();
        }

        // comboKeys formatted as "EMPLOYEEID:PARAMID" (e.g., "EMP01:15")
        String sql = getSelectColumnsWithJoins()
                + "WHERE aq.status = 'A' "
                + "  AND CONCAT(UPPER(TRIM(aq.employee_id)), ':', aq.param_id) IN (:comboKeys) "
                + "ORDER BY aq.qualification_id ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("comboKeys", comboKeys);

        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
