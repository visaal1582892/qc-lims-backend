package com.medplus.qc_lims_backend.dao.mapper;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AnalystQualificationRowMapper implements RowMapper<AnalystQualification> {

    @Override
    public AnalystQualification mapRow(ResultSet rs, int rowNum) throws SQLException {
        AnalystQualification qual = new AnalystQualification();
        qual.setId(rs.getLong("qualification_id"));
        qual.setEmployeeId(rs.getString("employee_id"));

        long paramId = rs.getLong("param_id");
        if (!rs.wasNull()) {
            TestParameter param = new TestParameter();
            param.setId(paramId);
            try {
                param.setName(rs.getString("test_parameter_name"));
            } catch (SQLException ignored) {
                // optional joined columns if not present
            }
            qual.setTestParameter(param);
        }

        Date qualifiedOn = rs.getDate("qualified_on");
        if (qualifiedOn != null) {
            qual.setQualifiedOn(qualifiedOn.toLocalDate());
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            qual.setStatus("I".equalsIgnoreCase(statusStr) ? MasterStatus.INACTIVE : MasterStatus.ACTIVE);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            qual.setCreatedAt(createdAt.toInstant());
        }
        qual.setCreatedBy(rs.getString("created_by"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            qual.setUpdatedAt(updatedAt.toInstant());
        }
        qual.setUpdatedBy(rs.getString("updated_by"));

        return qual;
    }
}
