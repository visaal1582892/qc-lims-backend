package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.TestParameterBulkRow;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.MetricUnitRepository;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import com.medplus.qc_lims_backend.service.TestParameterServiceInterface;
import com.medplus.qc_lims_backend.specifications.MasterSpecifications;
import com.medplus.qc_lims_backend.util.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestParameterServiceImpl implements TestParameterServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Name", "Result Type (TEXTUAL/METRICS)", "Metric Unit ID", "Action (A/U/D)"};

    private final TestParameterRepository repository;
    private final MetricUnitRepository metricUnitRepository;

    public TestParameterServiceImpl(TestParameterRepository repository,
                                    MetricUnitRepository metricUnitRepository) {
        this.repository = repository;
        this.metricUnitRepository = metricUnitRepository;
    }

    @Override
    public Page<TestParameter> findAll(String search, boolean includeInactive, Pageable pageable) {
        Specification<TestParameter> spec = MasterSpecifications.nameContains(search);
        if (includeInactive) {
            return repository.findAll(spec, pageable);
        }
        return repository.findByStatus(MasterStatus.ACTIVE, spec, pageable);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<TestParameterBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        Map<Long, TestParameter> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(TestParameter::getId, tp -> tp));
        Map<Long, MetricUnit> metricUnitsById = metricUnitRepository.findAll().stream()
                .collect(Collectors.toMap(MetricUnit::getId, mu -> mu));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, metricUnitsById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = ExcelUtils.createErrorReport(
                    "Test Parameters",
                    Arrays.copyOf(HEADERS, HEADERS.length),
                    rows, errors, (r, bulkRow, col) -> {
                        TestParameterBulkRow row = (TestParameterBulkRow) bulkRow;
                        r.createCell(col++).setCellValue(row.name() != null ? row.name() : "");
                        r.createCell(col++).setCellValue(row.resultType() != null ? row.resultType() : "");
                        if (row.metricUnitId() != null) {
                            r.createCell(col).setCellValue(row.metricUnitId().doubleValue());
                        }
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, metricUnitsById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Test Parameters", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<TestParameterBulkRow> parseFile(MultipartFile file) {
        List<TestParameterBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; rowNum++; continue; } // skip header row
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;
                // Skip completely blank rows
                String action = ExcelUtils.getCellString(row, 4);
                String name = ExcelUtils.getCellString(row, 1);
                String resultType = ExcelUtils.getCellString(row, 2);
                String metricUnitIdStr = ExcelUtils.getCellString(row, 3);
                if (action == null && name == null && resultType == null && metricUnitIdStr == null) {
                    rowNum++;
                    continue;
                }
                Long id = ExcelUtils.getCellLong(row, 0);
                Long metricUnitId = ExcelUtils.getCellLong(row, 3);
                rows.add(new TestParameterBulkRow(id, action != null ? action.trim() : "", name, resultType, metricUnitId, rowNum));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(TestParameterBulkRow row,
                                     Map<Long, TestParameter> existing,
                                     Map<Long, MetricUnit> metricUnits) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (errors.size() > 1) return errors;

        String action = row.action().toUpperCase();

        // For Deactivate, only ID + Action are needed
        if (!action.equals("D")) {
            if (row.name() == null || row.name().isBlank()) {
                errors.add("Name is required");
            }
            if (row.resultType() == null || !row.resultType().matches("(?i)TEXTUAL|METRICS")) {
                errors.add("Result type must be TEXTUAL or METRICS");
            }
            if (row.metricUnitId() != null && !metricUnits.containsKey(row.metricUnitId())) {
                errors.add(ErrorMessages.ERR_INVALID_METRIC_UNIT.getMessage());
            }
        }

        ExcelUtils.validateExists(row, existing, errors, "Test Parameter");

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<TestParameterBulkRow> rows,
            Map<Long, TestParameter> existingById,
            Map<Long, MetricUnit> metricUnitsById,
            String employeeId) {

        Map<String, List<TestParameterBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<TestParameterBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<TestParameter> toSave = adds.stream().map(r -> {
                MetricUnit mu = r.metricUnitId() != null ? metricUnitsById.get(r.metricUnitId()) : null;
                return TestParameter.builder()
                        .name(r.name())
                        .resultType(r.resultType())
                        .metricUnit(mu)
                        .status(MasterStatus.ACTIVE)
                        .build();
            }).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<TestParameterBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<TestParameter> toUpdate = updates.stream().map(r -> {
                TestParameter tp = existingById.get(r.id());
                tp.setName(r.name());
                tp.setResultType(r.resultType());
                MetricUnit mu = r.metricUnitId() != null ? metricUnitsById.get(r.metricUnitId()) : null;
                tp.setMetricUnit(mu);
                return tp;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<TestParameterBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(TestParameterBulkRow::id).toList();
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
