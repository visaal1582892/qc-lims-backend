package com.medplus.qc_lims_backend.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Filter criteria for Metric Specifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MetricSpecificationSearchCriteria extends BaseSearchCriteria {

    private Long configId;
    private String productId;
    private Long metricId;
    private String metricName;
    private String unit;

    // Backward-compatibility aliases
    public Long getMetricUnitId() {
        return metricId;
    }

    public void setMetricUnitId(Long metricUnitId) {
        this.metricId = metricUnitId;
    }

    public String getMetricUnitName() {
        return metricName;
    }

    public void setMetricUnitName(String metricUnitName) {
        this.metricName = metricUnitName;
    }
}
