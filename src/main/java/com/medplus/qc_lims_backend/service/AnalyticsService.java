package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.AnalyticsQueryRequest;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSampleResponse;
import com.medplus.qc_lims_backend.dto.response.AnalyticsSummaryResponse;
import com.medplus.qc_lims_backend.dto.response.PageResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.repository.AnalyticsRepository;
import com.medplus.qc_lims_backend.repository.AnalyticsSpecifications;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import com.medplus.qc_lims_backend.repository.TestParameterMasterRepository;
import com.medplus.qc_lims_backend.repository.TrfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Multi-dimensional analytics service (SRS Module 12.2).
 *
 * Provides paginated, filterable analytics queries across samples,
 * test execution, and parameters. Uses JpaSpecificationExecutor
 * for dynamic WHERE-clause composition.
 *
 * C-6: Resolves manufacturer_id to trf_ids before building specification,
 * since sample_inward_register has no manufacturer_id column.
 * C-7: Validates dateFrom <= dateTo at service layer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestParameterMasterRepository testParameterRepository;
    private final TrfRepository trfRepository;

    /**
     * Search samples with multi-dimensional filters and pagination (R-12).
     *
     * Filters are applied via Specification composition — only non-null
     * params generate WHERE predicates. Returns paginated PageResponse.
     */
    public PageResponse<AnalyticsSampleResponse> searchSamples(AnalyticsQueryRequest request) {
        // C-7: Validate date range
        if (request.dateFrom() != null && request.dateTo() != null
                && request.dateFrom().isAfter(request.dateTo())) {
            throw new BusinessRuleViolationException(
                    "dateFrom (" + request.dateFrom() + ") cannot be after dateTo (" + request.dateTo() + ").");
        }

        // C-6: Resolve manufacturer_id to list of trf_ids
        List<Integer> manufacturerTrfIds = null;
        if (request.manufacturerId() != null) {
            manufacturerTrfIds = trfRepository.findByManufacturerIdAndStatus(request.manufacturerId(), RecordStatus.ACTIVE)
                    .stream()
                    .map(t -> t.getId().intValue())
                    .collect(Collectors.toList());
            if (manufacturerTrfIds.isEmpty()) {
                return new PageResponse<>(List.of(), request.page(), request.size(), 0L, 0);
            }
        }

        // Sort field whitelist
        String sortBy = request.sortBy();
        String sortDirection = request.sortDirection();
        List<String> allowedSortFields = List.of("arNumber", "batchNo", "receiptDate");
        if (sortBy != null && !sortBy.isBlank() && !allowedSortFields.contains(sortBy)) {
            sortBy = "receiptDate";
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "receiptDate";
            sortDirection = "DESC";
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);

        // Build specification from non-null filters
        Specification<com.medplus.qc_lims_backend.domain.SampleInwardRegister> spec =
                AnalyticsSpecifications.composite(
                        manufacturerTrfIds,
                        request.batchNumber(),
                        request.dateFrom(),
                        request.dateTo(),
                        null // arStatus filter handled separately
                );

        // If executionStatus is provided, filter by matching test_execution records
        String executionStatus = request.executionStatus();
        if (executionStatus != null && !executionStatus.isBlank()) {
            List<com.medplus.qc_lims_backend.domain.TestExecution> matchingExecutions =
                    testExecutionRepository.findByExecutionStatusAndStatus(executionStatus, RecordStatus.ACTIVE);
            List<String> matchingArNumbers = matchingExecutions.stream()
                    .map(com.medplus.qc_lims_backend.domain.TestExecution::getArNumber)
                    .collect(Collectors.toList());

            if (matchingArNumbers.isEmpty()) {
                return new PageResponse<>(List.of(), request.page(), request.size(), 0L, 0);
            }

            spec = spec.and((root, query, cb) -> root.get("arNumber").in(matchingArNumbers));
        }

        Page<com.medplus.qc_lims_backend.domain.SampleInwardRegister> page =
                analyticsRepository.findAll(spec, pageable);

        // Convert to response DTOs
        List<AnalyticsSampleResponse> items = page.getContent().stream()
                .map(this::toAnalyticsResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Parameter-wise analytics summary (R-10).
     */
    public List<AnalyticsSummaryResponse> summaryByParameter() {
        List<Object[]> rows = testExecutionRepository.countTestsByParameter(RecordStatus.ACTIVE);

        List<com.medplus.qc_lims_backend.domain.TestParameterMaster> allParams =
                testParameterRepository.findByStatus(RecordStatus.ACTIVE);
        Map<Integer, String> paramNameMap = allParams.stream()
                .collect(Collectors.toMap(
                        p -> p.getId().intValue(),
                        com.medplus.qc_lims_backend.domain.TestParameterMaster::getParameterName,
                        (a, b) -> a
                ));

        return rows.stream().map(row -> {
            Integer paramId = ((Number) row[0]).intValue();
            Long total = ((Number) row[1]).longValue();

            long passed = testExecutionRepository.findByParameterIdAndStatus(paramId, RecordStatus.ACTIVE).stream()
                    .filter(te -> "PASS".equals(te.getComplianceFlag()))
                    .count();
            long failed = testExecutionRepository.findByParameterIdAndStatus(paramId, RecordStatus.ACTIVE).stream()
                    .filter(te -> "FAIL".equals(te.getComplianceFlag()))
                    .count();
            long pending = total - passed - failed;

            return new AnalyticsSummaryResponse(
                    paramId.longValue(),
                    paramNameMap.getOrDefault(paramId, "Unknown"),
                    total,
                    passed,
                    failed,
                    pending >= 0 ? pending : 0
            );
        }).collect(Collectors.toList());
    }

    /**
     * Analyst productivity summary for a date range (R-08).
     * O-2: Uses submitted_at for productivity rather than created_at
     * to reflect when tests were actually completed.
     */
    public List<AnalyticsSummaryResponse> summaryByAnalyst(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleViolationException("Both 'from' and 'to' date parameters are required.");
        }
        if (from.isAfter(to)) {
            throw new BusinessRuleViolationException("'from' date cannot be after 'to' date.");
        }

        java.time.Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        java.time.Instant toInstant = to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<Object[]> rows = testExecutionRepository.countTestsByAnalystAndDateRange(
                fromInstant, toInstant, RecordStatus.ACTIVE);

        return rows.stream().map(row -> {
            Integer analystId = ((Number) row[0]).intValue();
            Long total = ((Number) row[1]).longValue();
            return new AnalyticsSummaryResponse(
                    analystId.longValue(),
                    "Analyst-" + analystId,
                    total,
                    0L, 0L, 0L
            );
        }).collect(Collectors.toList());
    }

    // --- DTO conversion ---

    private AnalyticsSampleResponse toAnalyticsResponse(
            com.medplus.qc_lims_backend.domain.SampleInwardRegister inward) {

        // Look up first test execution for this sample to get execution details
        com.medplus.qc_lims_backend.domain.TestExecution execution = testExecutionRepository
                .findByArNumberAndStatus(inward.getArNumber(), RecordStatus.ACTIVE).stream()
                .findFirst()
                .orElse(null);

        String executionStatus = execution != null ? execution.getExecutionStatus() : null;
        String complianceFlag = execution != null ? execution.getComplianceFlag() : null;
        Integer analystId = execution != null ? execution.getAllocatedToAnalystId() : null;
        Integer parameterId = execution != null ? execution.getParameterId() : null;
        Date submittedAt = execution != null && execution.getSubmittedAt() != null
                ? Date.from(execution.getSubmittedAt()) : null;

        // Look up parameter name
        final String parameterName;
        if (parameterId != null) {
            java.util.Optional<com.medplus.qc_lims_backend.domain.TestParameterMaster> paramOpt =
                    testParameterRepository.findByIdAndStatus(parameterId.longValue(), RecordStatus.ACTIVE);
            parameterName = paramOpt.map(com.medplus.qc_lims_backend.domain.TestParameterMaster::getParameterName).orElse(null);
        } else {
            parameterName = null;
        }

        Date receiptDate = inward.getReceiptDate() != null
                ? Date.from(inward.getReceiptDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                : null;

        return new AnalyticsSampleResponse(
                inward.getId(),
                inward.getArNumber(),
                null, // manufacturer_id is on trf_master, not sample_inward_register
                inward.getBatchNo(),
                inward.getArStatus(),
                executionStatus,
                complianceFlag,
                parameterId != null ? parameterId.longValue() : null,
                parameterName,
                analystId,
                receiptDate,
                submittedAt
        );
    }
}
