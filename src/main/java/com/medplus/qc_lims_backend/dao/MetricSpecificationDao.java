package com.medplus.qc_lims_backend.dao;

import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.criteria.MetricSpecificationSearchCriteria;

import java.util.Collection;
import java.util.List;

public interface MetricSpecificationDao extends BaseDao<MetricSpecification, MetricSpecificationSearchCriteria> {

    /**
     * Exact composite combination lookup for active records (e.g. "CONFIGID:UNITID")
     * to prevent Cartesian product over-fetching during Excel bulk validation.
     */
    List<MetricSpecification> findActiveByConfigAndUnitCombos(Collection<String> comboKeys);
}
