package com.medplus.qc_lims_backend.dao.mapper;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TestParameterRowMapper implements RowMapper<TestParameter> {

    @Override
    public TestParameter mapRow(ResultSet rs, int rowNum) throws SQLException {
        TestParameter param = new TestParameter();
        param.setId(rs.getLong("param_id"));
        param.setName(rs.getString("name"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            param.setStatus("I".equalsIgnoreCase(statusStr) ? MasterStatus.INACTIVE : MasterStatus.ACTIVE);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            param.setCreatedAt(createdAt.toInstant());
        }
        param.setCreatedBy(rs.getString("created_by"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            param.setUpdatedAt(updatedAt.toInstant());
        }
        param.setUpdatedBy(rs.getString("updated_by"));

        return param;
    }
}
