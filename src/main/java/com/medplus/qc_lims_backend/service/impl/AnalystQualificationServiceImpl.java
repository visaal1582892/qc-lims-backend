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
import com.medplus.qc_lims_backend.util.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import com.medplus.qc_lims_backend.dao.AnalystQualificationDao;
import com.medplus.qc_lims_backend.domain.criteria.AnalystQualificationSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
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
    private final AnalystQualificationDao analystQualificationDao;

    public AnalystQualificationServiceImpl(AnalystQualificationRepository repository,
                                           TestParameterRepository parameterRepository,
                                           AnalystQualificationDao analystQualificationDao) {
        this.repository = repository;
        this.parameterRepository = parameterRepository;
        this.analystQualificationDao = analystQualificationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AnalystQualification> getPagedQualifications(AnalystQualificationSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new AnalystQualificationSearchCriteria();
        }
        long total = analystQualificationDao.count(criteria);
        if (total == 0) {
            return PagedResponse.of(List.of(), 0, criteria.getPage(), criteria.getSize());
        }

        int offset = criteria.getOffset();
        List<Long> ids = analystQualificationDao.getPageIds(criteria, offset, criteria.getSize());
        List<AnalystQualification> content = analystQualificationDao.getEntitiesByIds(ids);

        return PagedResponse.of(content, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalystQualification> findList(AnalystQualificationSearchCriteria criteria, int maxResults) {
        if (criteria == null) {
            criteria = new AnalystQualificationSearchCriteria();
        }
        return analystQualificationDao.findList(criteria, maxResults);
    }


    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<AnalystQualificationBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        List<Long> ids = rows.stream().map(AnalystQualificationBulkRow::id).filter(Objects::nonNull).distinct().toList();
        Map<Long, AnalystQualification> existingById = ids.isEmpty() ? Map.of() :
                repository.findAllById(ids).stream().collect(Collectors.toMap(AnalystQualification::getId, aq -> aq));

        List<Long> paramIds = rows.stream().map(AnalystQualificationBulkRow::parameterId).filter(Objects::nonNull).distinct().toList();
        Map<Long, TestParameter> paramsById = paramIds.isEmpty() ? Map.of() :
                parameterRepository.findAllById(paramIds).stream().collect(Collectors.toMap(TestParameter::getId, tp -> tp));

        // Extract combination keys (e.g., "EMP01:15") to query ONLY matching active records from DB via DAO,
        // and detect intra-file duplicate combination rows
        Set<String> comboKeys = new HashSet<>();
        Map<String, List<Integer>> rowsByCombo = new HashMap<>();

        for (AnalystQualificationBulkRow r : rows) {
            String act = r.action() != null ? r.action().toUpperCase() : "";
            if (act.equals("A")) {
                if (r.employeeId() != null && !r.employeeId().isBlank() && r.parameterId() != null) {
                    String combo = r.employeeId().trim().toUpperCase() + ":" + r.parameterId();
                    comboKeys.add(combo);
                    rowsByCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(r.rowNumber());
                }
            } else if (act.equals("U") && r.id() != null && existingById.containsKey(r.id())) {
                AnalystQualification ex = existingById.get(r.id());
                if (ex.getStatus() == MasterStatus.ACTIVE) {
                    String emp = (r.employeeId() != null && !r.employeeId().isBlank())
                            ? r.employeeId().trim().toUpperCase()
                            : (ex.getEmployeeId() != null ? ex.getEmployeeId().trim().toUpperCase() : null);
                    Long pid = r.parameterId() != null
                            ? r.parameterId()
                            : (ex.getTestParameter() != null ? ex.getTestParameter().getId() : null);
                    if (emp != null && pid != null) {
                        String combo = emp + ":" + pid;
                        comboKeys.add(combo);
                        rowsByCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(r.rowNumber());
                    }
                }
            }
        }

        Set<String> duplicateComboKeys = rowsByCombo.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<AnalystQualification> activeFromDb = analystQualificationDao.findActiveByEmployeeAndParamCombos(comboKeys);
        Map<String, List<AnalystQualification>> activeByCombo = activeFromDb.stream()
                .filter(aq -> aq.getEmployeeId() != null && aq.getTestParameter() != null)
                .collect(Collectors.groupingBy(aq ->
                        aq.getEmployeeId().trim().toUpperCase() + ":" + aq.getTestParameter().getId()));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, paramsById, activeByCombo, duplicateComboKeys);
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
                                     Map<Long, TestParameter> params,
                                     Map<String, List<AnalystQualification>> activeByCombo,
                                     Set<String> duplicateComboKeys) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (!errors.isEmpty()) return errors;

        String action = row.action().toUpperCase();

        // For Deactivate, only ID + Action are needed
        if (!action.equals("D")) {
            if (action.equals("A")) {
                if (row.employeeId() == null || row.employeeId().isBlank()) {
                    errors.add("Employee ID is required");
                }
                if (row.parameterId() == null) {
                    errors.add("Parameter ID is required");
                } else if (!params.containsKey(row.parameterId())) {
                    errors.add(ErrorMessages.ERR_PARAM_NOT_FOUND.getMessage());
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

                // Check active combination duplicate for 'A'
                if (row.employeeId() != null && !row.employeeId().isBlank() && row.parameterId() != null && params.containsKey(row.parameterId())) {
                    String comboKey = row.employeeId().trim().toUpperCase() + ":" + row.parameterId();
                    if (duplicateComboKeys.contains(comboKey)) {
                        errors.add(ErrorMessages.ERR_QUALIFICATION_DUPLICATE_IN_FILE.getMessage());
                    } else {
                        List<AnalystQualification> matching = activeByCombo.get(comboKey);
                        if (matching != null && !matching.isEmpty()) {
                            errors.add(ErrorMessages.ERR_QUALIFICATION_DUPLICATE.getMessage());
                        }
                    }
                }
            } else {
                // Update: at least one field required; validate provided fields
                if (row.qualifiedOn() != null && !row.qualifiedOn().isBlank()) {
                    try {
                        LocalDate.parse(row.qualifiedOn(), DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (DateTimeParseException e) {
                        errors.add("Qualified On must be in YYYY-MM-DD format");
                    }
                }
                if (row.parameterId() != null && !params.containsKey(row.parameterId())) {
                    errors.add(ErrorMessages.ERR_PARAM_NOT_FOUND.getMessage());
                }
                ExcelUtils.validateUpdateHasFields(row, errors,
                        "Employee ID", row.employeeId(), "Parameter ID",
                        String.valueOf(row.parameterId()), "Qualified On", row.qualifiedOn());

                // Check active combination duplicate for 'U'
                if (row.id() != null && existing.containsKey(row.id()) && existing.get(row.id()).getStatus() == MasterStatus.ACTIVE) {
                    AnalystQualification ex = existing.get(row.id());
                    String effEmp = (row.employeeId() != null && !row.employeeId().isBlank())
                            ? row.employeeId().trim().toUpperCase()
                            : (ex.getEmployeeId() != null ? ex.getEmployeeId().trim().toUpperCase() : null);
                    Long effParam = row.parameterId() != null
                            ? row.parameterId()
                            : (ex.getTestParameter() != null ? ex.getTestParameter().getId() : null);

                    if (effEmp != null && effParam != null && (row.parameterId() == null || params.containsKey(row.parameterId()))) {
                        String comboKey = effEmp + ":" + effParam;
                        if (duplicateComboKeys.contains(comboKey)) {
                            errors.add(ErrorMessages.ERR_QUALIFICATION_DUPLICATE_IN_FILE.getMessage());
                        } else {
                            List<AnalystQualification> matching = activeByCombo.get(comboKey);
                            if (matching != null && matching.stream().anyMatch(a -> !a.getId().equals(row.id()))) {
                                errors.add(ErrorMessages.ERR_QUALIFICATION_DUPLICATE.getMessage());
                            }
                        }
                    }
                }
            }
        }

        ExcelUtils.validateExists(row, existing, AnalystQualification::getStatus, errors, "Analyst Qualification");

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
            List<Long> updateIds = updates.stream().map(AnalystQualificationBulkRow::id).toList();
            repository.logBeforeUpdate(updateIds, employeeId);

            List<AnalystQualification> toUpdate = updates.stream().map(r -> {
                AnalystQualification aq = existingById.get(r.id());
                if (r.employeeId() != null && !r.employeeId().isBlank()) {
                    aq.setEmployeeId(r.employeeId());
                }
                if (r.parameterId() != null) {
                    TestParameter tp = paramsById.get(r.parameterId());
                    aq.setTestParameter(tp);
                }
                if (r.qualifiedOn() != null && !r.qualifiedOn().isBlank()) {
                    aq.setQualifiedOn(LocalDate.parse(r.qualifiedOn(), DateTimeFormatter.ISO_LOCAL_DATE));
                }
                return aq;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<AnalystQualificationBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(AnalystQualificationBulkRow::id).toList();
            repository.logBeforeUpdate(ids, employeeId);
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
