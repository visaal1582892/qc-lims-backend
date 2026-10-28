package com.medplus.qc_lims_backend.dao;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.criteria.InventoryConfigSearchCriteria;

import java.util.Collection;
import java.util.List;

public interface InventoryConfigurationDao extends BaseDao<InventoryConfiguration, InventoryConfigSearchCriteria> {

    /**
     * Exact composite combination lookup for active records (e.g. "PROD01:15")
     * to prevent Cartesian product over-fetching during Excel bulk validation.
     */
    List<InventoryConfiguration> findActiveByProductAndParamCombos(Collection<String> comboKeys);
}
