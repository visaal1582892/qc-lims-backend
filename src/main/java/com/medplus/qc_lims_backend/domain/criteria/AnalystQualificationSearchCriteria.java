package com.medplus.qc_lims_backend.domain.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filter criteria for Analyst Qualifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AnalystQualificationSearchCriteria extends BaseSearchCriteria {

    private String employeeId;
    private Long paramId;
    private String parameterName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate qualifiedOn;

    public Long getParameterId() {
        return paramId;
    }

    public void setParameterId(Long parameterId) {
        this.paramId = parameterId;
    }
}
