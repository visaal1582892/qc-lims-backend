package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.MetricUnit;
import com.medplus.qc_lims_backend.dto.BulkRow;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.MetricUnitBulkRow;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.MetricUnitRepository;
import com.medplus.qc_lims_backend.service.MetricUnitServiceInterface;
import com.medplus.qc_lims_backend.specifications.MasterSpecifications;
import com.medplus.qc_lims_backend.util.ExcelUtils;
import com.medplus.qc_lims_backend.util.ExcelUtils.RowCellWriter;
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
public class MetricUnitServiceImpl implements MetricUnitServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"Action (A/U/D)", "ID", "Name", "Sub Name"};

    private final MetricUnitRepository repository;

    public MetricUnitServiceImpl(MetricUnitRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<MetricUnit> findAll(String search, boolean includeInactive, Pageable pageable) {
        Specification<MetricUnit> spec = MasterSpecifications.nameOrSubNameContains(search);
        if (includeInactive) {
            return repository.findAll(spec, pageable);
        }
        return repository.findByStatus(MasterStatus.ACTIVE, spec, pageable);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<MetricUnitBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        Map<Long, MetricUnit> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(MetricUnit::getId, u -> u));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = ExcelUtils.createErrorReport(
                    "Metric Units", HEADERS, rows, errors, (r, bulkRow, col) -> {
                        MetricUnitBulkRow row = (MetricUnitBulkRow) bulkRow;
                        r.createCell(col++).setCellValue(row.name() != null ? row.name() : "");
                        r.createCell(col).setCellValue(row.subName() != null ? row.subName() : "");
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Metric Units", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<MetricUnitBulkRow> parseFile(MultipartFile file) {
        List<MetricUnitBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            for (Row row : sheet) {
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;
                String action = ExcelUtils.getCellString(row, 0);
                if (action != null && !action.isBlank()) {
                    Long id = ExcelUtils.getCellLong(row, 1);
                    String name = ExcelUtils.getCellString(row, 2);
                    String subName = ExcelUtils.getCellString(row, 3);
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
        ExcelUtils.validateActionAndId(row, errors);
        if (errors.size() > 1) return errors; // action is invalid, skip rest

        if (row.name() == null || row.name().isBlank()) {
            errors.add("Name is required");
        }

        ExcelUtils.validateExists(row, existing, errors, "Metric Unit");

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<MetricUnitBulkRow> rows,
            Map<Long, MetricUnit> existingById,
            String employeeId) {

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
}
