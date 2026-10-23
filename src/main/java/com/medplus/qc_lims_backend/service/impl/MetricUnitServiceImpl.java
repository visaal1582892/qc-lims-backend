package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.MetricUnitBulkRow;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.MetricUnitRepository;
import com.medplus.qc_lims_backend.service.MetricUnitServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class MetricUnitServiceImpl implements MetricUnitServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"Action (A/U/D)", "ID", "Name", "Sub Name"};

    private final MetricUnitRepository repository;

    public MetricUnitServiceImpl(MetricUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<MetricUnit> findAll(boolean includeInactive, Pageable pageable) {
        if (includeInactive) {
            return repository.findAll(pageable);
        }
        return repository.findByStatus(MasterStatus.ACTIVE, pageable);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }

        List<MetricUnitBulkRow> rows = parseFile(file);
        if (rows.isEmpty()) {
            throw new BadRequestException(ErrorMessages.ERR_NO_VALID_ROWS.getMessage());
        }
        if (rows.size() > MAX_ROWS) {
            throw new BadRequestException(ErrorMessages.ERR_FILE_TOO_LARGE.getMessage());
        }

        // Load existing data for bulk validation
        Map<Long, MetricUnit> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(MetricUnit::getId, u -> u));

        // Validate all rows
        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = generateErrorFile(rows, errors);
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        // Execute bulk operations
        Map<String, List<MetricUnitBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<MetricUnitBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<MetricUnit> toSave = adds.stream().map(r ->
                    MetricUnit.builder()
                            .name(r.name())
                            .subName(r.subName())
                            .status(MasterStatus.ACTIVE)
                            .build()
            ).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<MetricUnitBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<MetricUnit> toUpdate = updates.stream().map(r -> {
                MetricUnit existing = existingById.get(r.id());
                existing.setName(r.name());
                existing.setSubName(r.subName());
                return existing;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<MetricUnitBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(MetricUnitBulkRow::id).toList();
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        String summary = String.format("Success — A:%d U:%d D:%d", added, updated, deactivated);
        log.info("Metric Unit bulk upload: {}", summary);
        return BulkUploadResponse.success(summary);
    }

    @Override
    public byte[] downloadTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Metric Units");
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

    private List<MetricUnitBulkRow> parseFile(MultipartFile file) {
        List<MetricUnitBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1; // skip header
            for (Row row : sheet) {
                if (rowNum == sheet.getPhysicalNumberOfRows()) break;
                String action = getCellString(row, 0);
                Long id = getCellLong(row, 1);
                String name = getCellString(row, 2);
                String subName = getCellString(row, 3);
                if (action != null && !action.isBlank()) {
                    rows.add(new MetricUnitBulkRow(id, action.trim(), name, subName, rowNum));
                }
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(MetricUnitBulkRow row, Map<Long, MetricUnit> existing) {
        List<String> errors = new ArrayList<>();
        String action = row.action().toUpperCase();

        // Action validation
        if (!action.matches("[AUD]")) {
            errors.add(ErrorMessages.ERR_INVALID_ACTION.getMessage());
            return errors;
        }

        // ID rules
        if (action.equals("A") && row.id() != null) {
            errors.add(ErrorMessages.ERR_ID_NOT_ALLOWED.getMessage());
        }
        if ((action.equals("U") || action.equals("D")) && row.id() == null) {
            errors.add(ErrorMessages.ERR_MISSING_ID.getMessage());
        }

        // Name validation
        if (row.name() == null || row.name().isBlank()) {
            errors.add("Name is required");
        }

        // Existence check for U/D
        if ((action.equals("U") || action.equals("D")) && row.id() != null) {
            if (!existing.containsKey(row.id())) {
                errors.add(ErrorMessages.ERR_ENTITY_NOT_FOUND.getMessage());
            }
        }

        return errors;
    }

    private byte[] generateErrorFile(List<MetricUnitBulkRow> rows, Map<Integer, List<String>> errors) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Metric Units");
            Row header = sheet.createRow(0);
            String[] headers = {"Action (A/U/D)", "ID", "Name", "Sub Name", "Error Messages"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (MetricUnitBulkRow row : rows) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.action());
                if (row.id() != null) r.createCell(1).setCellValue(row.id().doubleValue());
                r.createCell(2).setCellValue(row.name() != null ? row.name() : "");
                r.createCell(3).setCellValue(row.subName() != null ? row.subName() : "");
                List<String> rowErrors = errors.get(row.rowNumber());
                if (rowErrors != null) {
                    r.createCell(4).setCellValue(String.join("; ", rowErrors));
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
