package com.medplus.qc_lims_backend.dao.mapper;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class InventoryConfigRowMapper implements RowMapper<InventoryConfiguration> {

    @Override
    public InventoryConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
        InventoryConfiguration config = new InventoryConfiguration();
        config.setId(rs.getLong("config_id"));
        config.setProductId(rs.getString("product_id"));

        long paramId = rs.getLong("param_id");
        if (!rs.wasNull()) {
            TestParameter param = new TestParameter();
            param.setId(paramId);
            try {
                param.setName(rs.getString("test_parameter_name"));
            } catch (SQLException ignored) {
                // optional joined columns if not present
            }
            config.setTestParameter(param);
        }

        config.setTestMethodId(rs.getString("test_method_id"));
        config.setIsCritical(rs.getString("is_critical"));
        config.setResultType(rs.getString("result_type"));
        config.setSpecificationText(rs.getString("specification_text"));
        config.setCountNeeded(rs.getInt("count_needed"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            config.setStatus("I".equalsIgnoreCase(statusStr) ? MasterStatus.INACTIVE : MasterStatus.ACTIVE);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            config.setCreatedAt(createdAt.toInstant());
        }
        config.setCreatedBy(rs.getString("created_by"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            config.setUpdatedAt(updatedAt.toInstant());
        }
        config.setUpdatedBy(rs.getString("updated_by"));

        return config;
    }
}
