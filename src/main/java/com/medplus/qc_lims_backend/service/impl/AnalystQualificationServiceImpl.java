package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.AnalystQualificationBulkRow;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.AnalystQualificationRepository;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import com.medplus.qc_lims_backend.service.AnalystQualificationServiceInterface;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalystQualificationServiceImpl implements AnalystQualificationServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Employee ID", "Parameter ID", "Qualified On (YYYY-MM-DD)", "Action (A/U/D)"};

    private final AnalystQualificationRepository repository;
    private final TestParameterRepository parameterRepository;

    public AnalystQualificationServiceImpl(AnalystQualificationRepository repository,
                                           TestParameterRepository parameterRepository) {
        this.repository = repository;
        this.parameterRepository = parameterRepository;
    }

    @Override
    public Page<AnalystQualification> findAll(String search, boolean includeInactive, Pageable pageable) {
        Specification<AnalystQualification> spec = MasterSpecifications.nameContains(search);
        if (includeInactive) {
            return repository.findAll(spec, pageable);
        }
        return repository.findByStatus(MasterStatus.ACTIVE, spec, pageable);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<AnalystQualificationBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        Map<Long, AnalystQualification> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(AnalystQualification::getId, aq -> aq));
        Map<Long, TestParameter> paramsById = parameterRepository.findAll().stream()
                .collect(Collectors.toMap(TestParameter::getId, tp -> tp));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, paramsById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = ExcelUtils.createErrorReport(
                    "Analyst Qualifications",
                    Arrays.copyOf(HEADERS, HEADERS.length),
                    rows, errors, (r, bulkRow, col) -> {
                        AnalystQualificationBulkRow row = (AnalystQualificationBulkRow) bulkRow;
                        r.createCell(col++).setCellValue(row.employeeId() != null ? row.employeeId() : "");
                        if (row.parameterId() != null) {
                            r.createCell(col++).setCellValue(row.parameterId().doubleValue());
                        } else {
                            col++;
                        }
                        r.createCell(col).setCellValue(row.qualifiedOn() != null ? row.qualifiedOn() : "");
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, paramsById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Analyst Qualifications", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<AnalystQualificationBulkRow> parseFile(MultipartFile file) {
        List<AnalystQualificationBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; rowNum++; continue; } // skip header row
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;
                // Skip completely blank rows
                String action = ExcelUtils.getCellString(row, 4);
                String employeeId = ExcelUtils.getCellString(row, 1);
                String parameterIdStr = ExcelUtils.getCellString(row, 2);
                String qualifiedOn = ExcelUtils.getCellString(row, 3);
                if (action == null && employeeId == null && parameterIdStr == null && qualifiedOn == null) {
                    rowNum++;
                    continue;
                }
                Long id = ExcelUtils.getCellLong(row, 0);
                Long parameterId = ExcelUtils.getCellLong(row, 2);
                rows.add(new AnalystQualificationBulkRow(id, action != null ? action.trim() : "", employeeId, parameterId, qualifiedOn, rowNum));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(AnalystQualificationBulkRow row,
                                     Map<Long, AnalystQualification> existing,
                                     Map<Long, TestParameter> params) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (errors.size() > 1) return errors;

        if (row.employeeId() == null || row.employeeId().isBlank()) {
            errors.add("Employee ID is required");
        }
        if (row.parameterId() == null) {
            errors.add("Parameter ID is required");
        }
        ExcelUtils.validateExists(row, existing, errors, "Analyst Qualification");
        if (row.parameterId() != null && !params.containsKey(row.parameterId())) {
            errors.add("The specified parameter ID does not exist");
        }
        if (row.qualifiedOn() == null || row.qualifiedOn().isBlank()) {
            errors.add("Qualified On date is required");
        } else {
            try {
                LocalDate.parse(row.qualifiedOn(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add("Qualified On must be in YYYY-MM-DD format");
            }
        }

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<AnalystQualificationBulkRow> rows,
            Map<Long, AnalystQualification> existingById,
            Map<Long, TestParameter> paramsById,
            String employeeId) {

        Map<String, List<AnalystQualificationBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<AnalystQualificationBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<AnalystQualification> toSave = adds.stream().map(r -> {
                TestParameter tp = paramsById.get(r.parameterId());
                return AnalystQualification.builder()
                        .employeeId(r.employeeId())
                        .testParameter(tp)
                        .qualifiedOn(LocalDate.parse(r.qualifiedOn(), DateTimeFormatter.ISO_LOCAL_DATE))
                        .status(MasterStatus.ACTIVE)
                        .build();
            }).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<AnalystQualificationBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<AnalystQualification> toUpdate = updates.stream().map(r -> {
                AnalystQualification aq = existingById.get(r.id());
                aq.setEmployeeId(r.employeeId());
                TestParameter tp = paramsById.get(r.parameterId());
                aq.setTestParameter(tp);
                aq.setQualifiedOn(LocalDate.parse(r.qualifiedOn(), DateTimeFormatter.ISO_LOCAL_DATE));
                return aq;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<AnalystQualificationBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(AnalystQualificationBulkRow::id).toList();
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
