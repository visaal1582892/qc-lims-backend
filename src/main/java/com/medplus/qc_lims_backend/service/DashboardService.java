package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.domain.TrfMaster;
import com.medplus.qc_lims_backend.dto.response.AnalystProductivityResponse;
import com.medplus.qc_lims_backend.dto.response.DashboardMetricsResponse;
import com.medplus.qc_lims_backend.dto.response.ManufacturerSampleResponse;
import com.medplus.qc_lims_backend.dto.response.ParameterAnalyticsResponse;
import com.medplus.qc_lims_backend.dto.response.StatusDistributionResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.repository.DashboardRepository;
import com.medplus.qc_lims_backend.repository.OosIncidentRepository;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final SampleInwardRepository sampleInwardRepository;
    private final TrfRepository trfRepository;
    private final TestParameterMasterRepository testParameterRepository;

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

    /**
     * Get all samples associated with a given manufacturer.
     *
     * Resolves manufacturer_id to TRF IDs, then finds all SampleInwardRegister
     * entries linked to those TRFs.
     *
     * @param manufacturerId the manufacturer ID
     * @return list of manufacturer sample responses
     */
    public List<ManufacturerSampleResponse> getManufacturerWise(Integer manufacturerId) {
        // Find all active TRFs for this manufacturer
        List<Long> trfIds = dashboardRepository.findTrfIdsByManufacturerId(manufacturerId);
        if (trfIds.isEmpty()) {
            return List.of();
        }

        // Build a lookup map from TRF id to TRF entity for enrichment
        List<TrfMaster> trfs = trfRepository.findByStatus(RecordStatus.ACTIVE);
        Map<Long, TrfMaster> trfMap = trfs.stream()
                .filter(t -> t.getManufacturerId() != null && t.getManufacturerId().equals(manufacturerId))
                .collect(Collectors.toMap(TrfMaster::getId, t -> t, (a, b) -> a));

        // Find all inward samples for these TRFs
        List<SampleInwardRegister> samples = sampleInwardRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .filter(s -> s.getTrfId() != null && trfIds.contains(s.getTrfId().longValue()))
                .collect(Collectors.toList());

        // Convert to response DTOs
        return samples.stream().map(s -> {
            TrfMaster trf = trfMap.get(s.getTrfId().longValue());
            return new ManufacturerSampleResponse(
                    s.getId(),
                    s.getArNumber(),
                    s.getBatchNo(),
                    s.getArStatus(),
                    trf != null ? trf.getTrfStatus() : null,
                    trf != null ? trf.getCompanyName() : null,
                    trf != null ? "Manufacturer-" + trf.getManufacturerId() : null
            );
        }).collect(Collectors.toList());
    }

    /**
     * Get analyst productivity within a date range.
     *
     * Returns test counts broken down by compliance outcome for the given analyst.
     *
     * @param analystId  the analyst ID
     * @param startDate  start of the date range (inclusive)
     * @param endDate    end of the date range (inclusive)
     * @return analyst productivity response
     */
    public AnalystProductivityResponse getAnalystProductivity(Integer analystId, LocalDate startDate, LocalDate endDate) {
        List<TestExecution> executions = testExecutionRepository.findByAllocatedToAnalystIdAndStatus(analystId, RecordStatus.ACTIVE);

        long totalTests = 0;
        long completed = 0;
        long passed = 0;
        long failed = 0;
        long pending = 0;
        long onHold = 0;

        for (TestExecution te : executions) {
            // Filter by date range on createdAt
            if (te.getCreatedAt() != null) {
                LocalDate execDate = te.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if ((startDate == null || !execDate.isBefore(startDate))
                        && (endDate == null || !execDate.isAfter(endDate))) {
                    totalTests++;
                    if ("Released".equals(te.getExecutionStatus()) || "Under_Review".equals(te.getExecutionStatus())) {
                        completed++;
                        if ("PASS".equals(te.getComplianceFlag())) {
                            passed++;
                        } else if ("FAIL".equals(te.getComplianceFlag())) {
                            failed++;
                        }
                    } else if ("Hold".equals(te.getExecutionStatus())) {
                        onHold++;
                    } else {
                        pending++;
                    }
                }
            }
        }

        return new AnalystProductivityResponse(
                analystId,
                totalTests,
                completed,
                passed,
                failed,
                pending,
                onHold
        );
    }

    /**
     * Track samples by date range and/or batch number.
     *
     * @param startDate start of date range (optional)
     * @param endDate   end of date range (optional)
     * @param batchNo   batch number to filter by (optional)
     * @return list of matching samples
     */
    public List<ManufacturerSampleResponse> getDateBatchTracking(LocalDate startDate, LocalDate endDate, String batchNo) {
        List<SampleInwardRegister> samples = dashboardRepository.findByDateRangeAndBatch(
                startDate, endDate, batchNo != null && !batchNo.isBlank() ? batchNo : null);

        // Build TRF lookup for enrichment
        Map<Integer, TrfMaster> trfMap = trfRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        t -> t.getId() != null ? t.getId().intValue() : -1,
                        t -> t,
                        (a, b) -> a
                ));

        return samples.stream().map(s -> {
            TrfMaster trf = s.getTrfId() != null ? trfMap.get(s.getTrfId()) : null;
            return new ManufacturerSampleResponse(
                    s.getId(),
                    s.getArNumber(),
                    s.getBatchNo(),
                    s.getArStatus(),
                    trf != null ? trf.getTrfStatus() : null,
                    trf != null ? trf.getCompanyName() : null,
                    trf != null ? "Manufacturer-" + trf.getManufacturerId() : null
            );
        }).collect(Collectors.toList());
    }

    /**
     * Get analytics for a specific test parameter.
     *
     * @param parameterId the test parameter ID
     * @return parameter analytics with pass/fail/pending breakdown
     */
    public ParameterAnalyticsResponse getParameterAnalytics(Integer parameterId) {
        // Fetch parameter details
        var paramOpt = testParameterRepository.findByIdAndStatus(parameterId.longValue(), RecordStatus.ACTIVE);
        if (paramOpt.isEmpty()) {
            return new ParameterAnalyticsResponse(
                    parameterId.longValue(), "Unknown", 0L, 0L, 0L, 0L, 0L);
        }

        String parameterName = paramOpt.get().getParameterName();

        // Get compliance breakdown
        List<Object[]> rows = dashboardRepository.countByParameterAndCompliance(parameterId);
        long total = 0;
        long passed = 0;
        long failed = 0;
        long hold = 0;

        for (Object[] row : rows) {
            String flag = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;
            if ("PASS".equals(flag)) {
                passed = count;
            } else if ("FAIL".equals(flag)) {
                failed = count;
            }
        }

        // Get hold count for this parameter
        hold = testExecutionRepository.countByExecutionStatusAndStatus("Hold", RecordStatus.ACTIVE);
        long pending = total - passed - failed - hold;
        if (pending < 0) pending = 0;

        return new ParameterAnalyticsResponse(
                parameterId.longValue(),
                parameterName,
                total,
                passed,
                failed,
                pending,
                hold
        );
    }

    /**
     * Filter and count samples by lifecycle status.
     *
     * @param status the AR status to filter by
     * @return list of status distribution entries
     */
    public List<StatusDistributionResponse> getStatusFiltering(String status) {
        if (status == null || status.isBlank()) {
            // Return all status groups if no specific status is requested
            List<Object[]> groups = dashboardRepository.countByArStatusGroups();
            return groups.stream().map(row ->
                    new StatusDistributionResponse(
                            (String) row[0],
                            ((Number) row[1]).longValue()
                    )
            ).collect(Collectors.toList());
        }

        // Return count for specific status
        long count = sampleInwardRepository.findByArStatusAndStatus(status, RecordStatus.ACTIVE).size();
        return List.of(new StatusDistributionResponse(status, count));
    }
}
