package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.InventoryConfigurationBulkRow;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.InventoryConfigurationRepository;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import com.medplus.qc_lims_backend.service.InventoryConfigurationServiceInterface;
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
public class InventoryConfigurationServiceImpl implements InventoryConfigurationServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Product ID", "Parameter ID", "Count Needed", "Action (A/U/D)"};

    private final InventoryConfigurationRepository repository;
    private final TestParameterRepository parameterRepository;

    public InventoryConfigurationServiceImpl(InventoryConfigurationRepository repository,
                                             TestParameterRepository parameterRepository) {
        this.repository = repository;
        this.parameterRepository = parameterRepository;
    }

    @Override
    public Page<InventoryConfiguration> findAll(String search, boolean includeInactive, Pageable pageable) {
        Specification<InventoryConfiguration> spec = MasterSpecifications.productIdContains(search);
        if (includeInactive) {
            return repository.findAll(spec, pageable);
        }
        return repository.findByStatus(MasterStatus.ACTIVE, spec, pageable);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<InventoryConfigurationBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        Map<Long, InventoryConfiguration> existingById = repository.findAll().stream()
                .collect(Collectors.toMap(InventoryConfiguration::getId, ic -> ic));
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
                    "Inventory Configuration",
                    Arrays.copyOf(HEADERS, HEADERS.length),
                    rows, errors, (r, bulkRow, col) -> {
                        InventoryConfigurationBulkRow row = (InventoryConfigurationBulkRow) bulkRow;
                        r.createCell(col++).setCellValue(row.productId() != null ? row.productId() : "");
                        if (row.parameterId() != null) {
                            r.createCell(col++).setCellValue(row.parameterId().doubleValue());
                        } else {
                            col++;
                        }
                        if (row.countNeeded() != null) {
                            r.createCell(col).setCellValue(row.countNeeded().doubleValue());
                        }
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, paramsById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Inventory Configuration", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<InventoryConfigurationBulkRow> parseFile(MultipartFile file) {
        List<InventoryConfigurationBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            for (Row row : sheet) {
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;
                String idStr = ExcelUtils.getCellString(row, 0);
                if (idStr != null && !idStr.isBlank()) {
                    Long id = ExcelUtils.getCellLong(row, 0);
                    String productId = ExcelUtils.getCellString(row, 1);
                    Long parameterId = ExcelUtils.getCellLong(row, 2);
                    Integer countNeeded = ExcelUtils.getCellInt(row, 3);
                    String action = ExcelUtils.getCellString(row, 4);
                    if (action != null && !action.isBlank()) {
                        rows.add(new InventoryConfigurationBulkRow(id, action.trim(), productId, parameterId, countNeeded, rowNum));
                    }
                }
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(InventoryConfigurationBulkRow row,
                                     Map<Long, InventoryConfiguration> existing,
                                     Map<Long, TestParameter> params) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (errors.size() > 1) return errors;

        if (row.productId() == null || row.productId().isBlank()) {
            errors.add("Product ID is required");
        }
        if (row.parameterId() == null) {
            errors.add("Parameter ID is required");
        }
        if (row.countNeeded() == null || row.countNeeded() <= 0) {
            errors.add(ErrorMessages.ERR_COUNT_MUST_BE_POSITIVE.getMessage());
        }
        ExcelUtils.validateExists(row, existing, errors, "Inventory Configuration");
        if (row.parameterId() != null && !params.containsKey(row.parameterId())) {
            errors.add("The specified parameter ID does not exist");
        }

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<InventoryConfigurationBulkRow> rows,
            Map<Long, InventoryConfiguration> existingById,
            Map<Long, TestParameter> paramsById,
            String employeeId) {

        Map<String, List<InventoryConfigurationBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<InventoryConfigurationBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<InventoryConfiguration> toSave = adds.stream().map(r -> {
                TestParameter tp = paramsById.get(r.parameterId());
                return InventoryConfiguration.builder()
                        .productId(r.productId())
                        .testParameter(tp)
                        .countNeeded(r.countNeeded())
                        .status(MasterStatus.ACTIVE)
                        .build();
            }).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<InventoryConfigurationBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<InventoryConfiguration> toUpdate = updates.stream().map(r -> {
                InventoryConfiguration ic = existingById.get(r.id());
                ic.setProductId(r.productId());
                TestParameter tp = paramsById.get(r.parameterId());
                ic.setTestParameter(tp);
                ic.setCountNeeded(r.countNeeded());
                return ic;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<InventoryConfigurationBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(InventoryConfigurationBulkRow::id).toList();
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
