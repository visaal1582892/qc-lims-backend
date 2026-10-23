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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestParameterServiceImpl implements TestParameterServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"Action (A/U/D)", "ID", "Name", "Result Type (TEXTUAL/METRICS)", "Metric Unit ID"};

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
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }

        List<TestParameterBulkRow> rows = parseFile(file);
        if (rows.isEmpty()) {
            throw new BadRequestException(ErrorMessages.ERR_NO_VALID_ROWS.getMessage());
        }
        if (rows.size() > MAX_ROWS) {
            throw new BadRequestException(ErrorMessages.ERR_FILE_TOO_LARGE.getMessage());
        }

        // Bulk-load existing data for validation
        Map<Long, TestParameter> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(TestParameter::getId, tp -> tp));
        Map<Long, MetricUnit> metricUnitsById = metricUnitRepository.findAll().stream()
                .collect(Collectors.toMap(MetricUnit::getId, mu -> mu));

        // Validate
        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, metricUnitsById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = generateErrorFile(rows, errors);
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        // Execute
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

    @Override
    public byte[] downloadTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test Parameters");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                wb.write(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate template");
        }
    }

    // ---- Private helpers ----

    private List<TestParameterBulkRow> parseFile(MultipartFile file) {
        List<TestParameterBulkRow> rows = new ArrayList<>();
        try (Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            for (Row row : sheet) {
                if (rowNum == sheet.getPhysicalNumberOfRows()) break;
                String action = getCellString(row, 0);
                Long id = getCellLong(row, 1);
                String name = getCellString(row, 2);
                String resultType = getCellString(row, 3);
                Long metricUnitId = getCellLong(row, 4);
                if (action != null && !action.isBlank()) {
                    rows.add(new TestParameterBulkRow(id, action.trim(), name, resultType, metricUnitId, rowNum));
                }
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
        String action = row.action().toUpperCase();

        if (!action.matches("[AUD]")) {
            errors.add(ErrorMessages.ERR_INVALID_ACTION.getMessage());
            return errors;
        }

        if (action.equals("A") && row.id() != null) {
            errors.add(ErrorMessages.ERR_ID_NOT_ALLOWED.getMessage());
        }
        if ((action.equals("U") || action.equals("D")) && row.id() == null) {
            errors.add(ErrorMessages.ERR_MISSING_ID.getMessage());
        }
        if (row.name() == null || row.name().isBlank()) {
            errors.add("Name is required");
        }
        if (row.resultType() == null || !row.resultType().matches("(?i)TEXTUAL|METRICS")) {
            errors.add("Result type must be TEXTUAL or METRICS");
        }
        if ((action.equals("U") || action.equals("D")) && row.id() != null && !existing.containsKey(row.id())) {
            errors.add(ErrorMessages.ERR_PARAM_NOT_FOUND.getMessage());
        }
        if (row.metricUnitId() != null && !metricUnits.containsKey(row.metricUnitId())) {
            errors.add(ErrorMessages.ERR_INVALID_METRIC_UNIT.getMessage());
        }

        return errors;
    }

    private byte[] generateErrorFile(List<TestParameterBulkRow> rows, Map<Integer, List<String>> errors) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test Parameters");
            Row header = sheet.createRow(0);
            String[] headers = {"Action (A/U/D)", "ID", "Name", "Result Type", "Metric Unit ID", "Error Messages"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (TestParameterBulkRow row : rows) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.action());
                if (row.id() != null) r.createCell(1).setCellValue(row.id().doubleValue());
                r.createCell(2).setCellValue(row.name() != null ? row.name() : "");
                r.createCell(3).setCellValue(row.resultType() != null ? row.resultType() : "");
                if (row.metricUnitId() != null) r.createCell(4).setCellValue(row.metricUnitId().doubleValue());
                List<String> rowErrors = errors.get(row.rowNumber());
                if (rowErrors != null) {
                    r.createCell(5).setCellValue(String.join("; ", rowErrors));
                }
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                wb.write(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate error report: " + e.getMessage());
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return cell.toString().trim();
    }

    private Long getCellLong(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return Math.round(cell.getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }
}
