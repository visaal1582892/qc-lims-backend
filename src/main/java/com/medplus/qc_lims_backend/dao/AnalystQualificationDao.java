package com.medplus.qc_lims_backend.dao;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.domain.criteria.AnalystQualificationSearchCriteria;

import java.util.Collection;
import java.util.List;

public interface AnalystQualificationDao extends BaseDao<AnalystQualification, AnalystQualificationSearchCriteria> {

    /**
     * Exact composite combination lookup for active records (e.g. "EMP01:15")
     * to prevent Cartesian product over-fetching during Excel bulk validation.
     */
    List<AnalystQualification> findActiveByEmployeeAndParamCombos(Collection<String> comboKeys);
}
