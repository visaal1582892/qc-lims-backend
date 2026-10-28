package com.medplus.qc_lims_backend.dao.mapper;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MetricSpecificationRowMapper implements RowMapper<MetricSpecification> {

    @Override
    public MetricSpecification mapRow(ResultSet rs, int rowNum) throws SQLException {
        MetricSpecification spec = new MetricSpecification();
        spec.setId(rs.getLong("metric_specification_id"));

        long configId = rs.getLong("config_id");
        if (!rs.wasNull()) {
            InventoryConfiguration config = new InventoryConfiguration();
            config.setId(configId);
            try {
                config.setProductId(rs.getString("product_id"));
                config.setTestMethodId(rs.getString("test_method_id"));
                String paramName = rs.getString("test_parameter_name");
                if (paramName != null) {
                    TestParameter param = new TestParameter();
                    param.setName(paramName);
                    config.setTestParameter(param);
                }
            } catch (SQLException ignored) {
                // optional joined columns if not present
            }
            spec.setInventoryConfiguration(config);
        }

        long metricId = 0;
        try {
            metricId = rs.getLong("metric_id");
        } catch (SQLException e) {
            metricId = rs.getLong("metric_unit_id");
        }

        if (!rs.wasNull()) {
            Metric metric = new Metric();
            metric.setId(metricId);
            try {
                String metricName = null;
                try {
                    metricName = rs.getString("metric_name");
                } catch (SQLException ignored) {}
                if (metricName == null) {
                    metricName = rs.getString("metric_unit_name");
                }
                metric.setMetricName(metricName);

                String unit = null;
                try {
                    unit = rs.getString("metric_unit");
                } catch (SQLException ignored) {}
                if (unit == null) {
                    unit = rs.getString("metric_unit_sub_name");
                }
                metric.setUnit(unit);
            } catch (SQLException ignored) {
                // optional joined columns if not present
            }
            spec.setMetric(metric);
        }

        BigDecimal lowerBound = rs.getBigDecimal("lower_bound");
        if (lowerBound != null) {
            spec.setLowerBound(lowerBound.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        BigDecimal upperBound = rs.getBigDecimal("upper_bound");
        if (upperBound != null) {
            spec.setUpperBound(upperBound.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            spec.setStatus("I".equalsIgnoreCase(statusStr) ? MasterStatus.INACTIVE : MasterStatus.ACTIVE);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            spec.setCreatedAt(createdAt.toInstant());
        }
        spec.setCreatedBy(rs.getString("created_by"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            spec.setUpdatedAt(updatedAt.toInstant());
        }
        spec.setUpdatedBy(rs.getString("updated_by"));

        return spec;
    }
}
