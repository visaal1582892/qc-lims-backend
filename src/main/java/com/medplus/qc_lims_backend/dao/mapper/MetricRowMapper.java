package com.medplus.qc_lims_backend.dao.mapper;

import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MetricRowMapper implements RowMapper<Metric> {

    @Override
    public Metric mapRow(ResultSet rs, int rowNum) throws SQLException {
        Metric metric = new Metric();
        metric.setId(rs.getLong("metric_id"));
        metric.setMetricName(rs.getString("metric_name"));
        metric.setUnit(rs.getString("unit"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            metric.setStatus("I".equalsIgnoreCase(statusStr) ? MasterStatus.INACTIVE : MasterStatus.ACTIVE);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            metric.setCreatedAt(createdAt.toInstant());
        }
        metric.setCreatedBy(rs.getString("created_by"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            metric.setUpdatedAt(updatedAt.toInstant());
        }
        metric.setUpdatedBy(rs.getString("updated_by"));

        return metric;
    }
}
