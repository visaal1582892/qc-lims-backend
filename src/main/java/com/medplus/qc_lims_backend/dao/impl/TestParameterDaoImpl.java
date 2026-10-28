package com.medplus.qc_lims_backend.dao.impl;

import com.medplus.qc_lims_backend.dao.TestParameterDao;
import com.medplus.qc_lims_backend.dao.mapper.TestParameterRowMapper;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.domain.criteria.TestParameterSearchCriteria;
import com.medplus.qc_lims_backend.util.SqlUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Map;

@Repository
public class TestParameterDaoImpl extends AbstractMasterDao<TestParameter, TestParameterSearchCriteria> implements TestParameterDao {

    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "updatedat", "COALESCE(tp.updated_at, tp.created_at)",
            "createdat", "tp.created_at",
            "id", "tp.param_id",
            "name", "tp.name",
            "status", "tp.status"
    );

    // Singleton RowMapper instance to avoid allocation per query execution
    private final TestParameterRowMapper rowMapper = new TestParameterRowMapper();

    public TestParameterDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getFromClauseWithJoins() {
        return "test_parameter_master tp";
    }

    @Override
    protected String getPrimaryKeyColumn() {
        return "tp.param_id";
    }

    @Override
    protected String getStatusColumn() {
        return "tp.status";
    }

    @Override
    protected String getSelectColumnsWithJoins() {
        return "SELECT tp.param_id, tp.name, tp.status, "
                + "tp.created_at, tp.created_by, tp.updated_at, tp.updated_by "
                + "FROM test_parameter_master tp ";
    }

    @Override
    protected RowMapper<TestParameter> getRowMapper() {
        return rowMapper;
    }

    @Override
    protected Map<String, String> getSortWhitelist() {
        return SORT_WHITELIST;
    }

    @Override
    protected void appendDomainFilters(TestParameterSearchCriteria criteria, StringBuilder where, MapSqlParameterSource params) {
        if (StringUtils.hasText(criteria.getName())) {
            where.append(" AND LOWER(tp.name) LIKE :name ");
            params.addValue("name", SqlUtils.escapeLike(criteria.getName()));
        }
    }

    @Override
    protected void appendSearchKeyFilter(String searchKey, StringBuilder where, MapSqlParameterSource params) {
        where.append(" AND LOWER(tp.name) LIKE :searchKey ");
        params.addValue("searchKey", SqlUtils.escapeLike(searchKey));
    }
}
