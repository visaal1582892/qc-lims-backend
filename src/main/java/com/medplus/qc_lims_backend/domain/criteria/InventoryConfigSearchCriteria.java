package com.medplus.qc_lims_backend.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Filter criteria for QC Inventory Configurations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryConfigSearchCriteria extends BaseSearchCriteria {

    private String productId;
    private Long paramId;
    private String parameterName;
    private String testMethodId;
    private String isCritical;
    private String resultType;
    private Integer countNeeded;

    public Long getParameterId() {
        return paramId;
    }

    public void setParameterId(Long parameterId) {
        this.paramId = parameterId;
    }
}
