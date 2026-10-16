package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.response.DashboardMetricsResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.repository.DashboardRepository;
import com.medplus.qc_lims_backend.repository.OosIncidentRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard aggregation service (SRS Module 12.1).
 *
 * Computes KPI metrics from trf_master, test_execution, and oos_incident_register.
 * All counts default to 0 when tables are empty — never returns null.
 *
 * Endpoints served by DashboardController under ROLE_DASHBOARD_VIEW.
 *
 * Correction C-2: Uses String literals for trfStatus (not enum) since TrfMaster.trfStatus is String.
 * Correction C-3: qualityRejected sourced from sample_inward_register.ar_status = 'Rejected'.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final OosIncidentRepository oosIncidentRepository;

    /**
     * Aggregate all dashboard KPI metrics in a single call.
     *
     * Queries:
     * - 3x trf_status counts (Inward Requests)
     * - 3x execution_status counts (Active Execution)
     * - 3x quality outcome counts (Quality Outcomes)
     * - 1x active OOS count
     * - 1x rework cases count
     *
     * Total: ~12 COUNT queries. With V9 indexes, each is an index scan.
     */
    public DashboardMetricsResponse getMetrics() {
        // Inward Requests (R-01) — from trf_master
        long pendingFeasibility = dashboardRepository.countByTrfStatus("Pending_Feasibility");
        long accepted = dashboardRepository.countByTrfStatus("Accepted");
        long rejected = dashboardRepository.countByTrfStatus("Rejected");

        // Active Execution Metrics (R-02) — from test_execution
        long testing = testExecutionRepository.countByExecutionStatusAndStatus("Testing", RecordStatus.ACTIVE);
        long onHold = testExecutionRepository.countByExecutionStatusAndStatus("Hold", RecordStatus.ACTIVE);
        long underReview = testExecutionRepository.countByExecutionStatusAndStatus("Under_Review", RecordStatus.ACTIVE);

        // Quality Outcomes (R-03)
        long released = testExecutionRepository.countByExecutionStatusAndStatus("Released", RecordStatus.ACTIVE);
        // C-3: qualityRejected from sample_inward_register.ar_status = 'Rejected'
        long qualityRejected = dashboardRepository.countRejectedSamples();
        // C-3: qualityOnHold from test_execution where execution_status = 'Hold'
        long qualityOnHold = dashboardRepository.countQualityOnHold();

        // Active OOS Incidents (R-04)
        long activeOosIncidents = oosIncidentRepository.countActiveOosIncidents(RecordStatus.ACTIVE);

        // Active Rework Cases (R-05)
        long activeReworkCases = testExecutionRepository.countByExecutionStatusAndStatus("Rework_Requested", RecordStatus.ACTIVE);

        return new DashboardMetricsResponse(
                pendingFeasibility,
                accepted,
                rejected,
                testing,
                onHold,
                underReview,
                released,
                qualityRejected,
                qualityOnHold,
                activeOosIncidents,
                activeReworkCases
        );
    }
}
