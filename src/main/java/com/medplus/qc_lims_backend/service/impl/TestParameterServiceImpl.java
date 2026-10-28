package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.TestParameterBulkRow;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import com.medplus.qc_lims_backend.service.TestParameterServiceInterface;
import com.medplus.qc_lims_backend.util.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.medplus.qc_lims_backend.dao.TestParameterDao;
import com.medplus.qc_lims_backend.domain.criteria.TestParameterSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestParameterServiceImpl implements TestParameterServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Name", "Action (A/U/D)"};

    private final TestParameterRepository repository;
    private final TestParameterDao testParameterDao;

    public TestParameterServiceImpl(TestParameterRepository repository,
                                    TestParameterDao testParameterDao) {
        this.repository = repository;
        this.testParameterDao = testParameterDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TestParameter> getPagedTestParameters(TestParameterSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new TestParameterSearchCriteria();
        }
        long total = testParameterDao.count(criteria);
        if (total == 0) {
            return PagedResponse.of(List.of(), 0, criteria.getPage(), criteria.getSize());
        }

        int offset = criteria.getOffset();
        List<Long> ids = testParameterDao.getPageIds(criteria, offset, criteria.getSize());
        List<TestParameter> content = testParameterDao.getEntitiesByIds(ids);

        return PagedResponse.of(content, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestParameter> findList(TestParameterSearchCriteria criteria, int maxResults) {
        if (criteria == null) {
            criteria = new TestParameterSearchCriteria();
        }
        return testParameterDao.findList(criteria, maxResults);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<TestParameterBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        List<Long> ids = rows.stream().map(TestParameterBulkRow::id).filter(Objects::nonNull).distinct().toList();
        Map<Long, TestParameter> existingById = ids.isEmpty() ? Map.of() :
                repository.findAllById(ids).stream().collect(Collectors.toMap(TestParameter::getId, tp -> tp));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById);
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
                        r.createCell(col).setCellValue(row.name() != null ? row.name() : "");
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, employeeId);
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
                String action = ExcelUtils.getCellString(row, 2);
                String name = ExcelUtils.getCellString(row, 1);
                if (action == null && name == null) {
                    rowNum++;
                    continue;
                }
                Long id = ExcelUtils.getCellLong(row, 0);
                rows.add(new TestParameterBulkRow(id, action != null ? action.trim() : "", name, rowNum));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(TestParameterBulkRow row,
                                     Map<Long, TestParameter> existing) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (!errors.isEmpty()) return errors;

        String action = row.action().toUpperCase();

        // For Deactivate, only ID + Action are needed
        if (!action.equals("D")) {
            if (action.equals("A")) {
                if (row.name() == null || row.name().isBlank()) {
                    errors.add("Name is required");
                }
            } else {
                // Update: at least one field required
                ExcelUtils.validateUpdateHasFields(row, errors, "Name", row.name());
            }
        }

        ExcelUtils.validateExists(row, existing, TestParameter::getStatus, errors, "Test Parameter");

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<TestParameterBulkRow> rows,
            Map<Long, TestParameter> existingById,
            String employeeId) {

        Map<String, List<TestParameterBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<TestParameterBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<TestParameter> toSave = adds.stream().map(r ->
                TestParameter.builder()
                        .name(r.name())
                        .status(MasterStatus.ACTIVE)
                        .build()
            ).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<TestParameterBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<Long> updateIds = updates.stream().map(TestParameterBulkRow::id).toList();
            repository.logBeforeUpdate(updateIds, employeeId);

            List<TestParameter> toUpdate = updates.stream().map(r -> {
                TestParameter tp = existingById.get(r.id());
                if (r.name() != null && !r.name().isBlank()) {
                    tp.setName(r.name());
                }
                return tp;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<TestParameterBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(TestParameterBulkRow::id).toList();
            repository.logBeforeUpdate(ids, employeeId);
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
