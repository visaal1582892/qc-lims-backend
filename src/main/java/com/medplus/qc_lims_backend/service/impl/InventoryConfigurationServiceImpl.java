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
public class InventoryConfigurationServiceImpl implements InventoryConfigurationServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"Action (A/U/D)", "ID", "Product ID", "Parameter ID", "Count Needed"};

    private final InventoryConfigurationRepository repository;
    private final TestParameterRepository parameterRepository;

    public InventoryConfigurationServiceImpl(InventoryConfigurationRepository repository,
                                             TestParameterRepository parameterRepository) {
        this.repository = repository;
        this.parameterRepository = parameterRepository;
    }

    @Override
    public Page<InventoryConfiguration> findAll(boolean includeInactive, Pageable pageable) {
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

        List<InventoryConfigurationBulkRow> rows = parseFile(file);
        if (rows.isEmpty()) {
            throw new BadRequestException(ErrorMessages.ERR_NO_VALID_ROWS.getMessage());
        }
        if (rows.size() > MAX_ROWS) {
            throw new BadRequestException(ErrorMessages.ERR_FILE_TOO_LARGE.getMessage());
        }

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
            byte[] errorFile = generateErrorFile(rows, errors);
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

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

    @Override
    public byte[] downloadTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventory Configuration");
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

    private List<InventoryConfigurationBulkRow> parseFile(MultipartFile file) {
        List<InventoryConfigurationBulkRow> rows = new ArrayList<>();
        try (Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            for (Row row : sheet) {
                if (rowNum == sheet.getPhysicalNumberOfRows()) break;
                String action = getCellString(row, 0);
                Long id = getCellLong(row, 1);
                String productId = getCellString(row, 2);
                Long parameterId = getCellLong(row, 3);
                Integer countNeeded = getCellInt(row, 4);
                if (action != null && !action.isBlank()) {
                    rows.add(new InventoryConfigurationBulkRow(id, action.trim(), productId, parameterId, countNeeded, rowNum));
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
        if (row.productId() == null || row.productId().isBlank()) {
            errors.add("Product ID is required");
        }
        if (row.parameterId() == null) {
            errors.add("Parameter ID is required");
        }
        if (row.countNeeded() == null || row.countNeeded() <= 0) {
            errors.add(ErrorMessages.ERR_COUNT_MUST_BE_POSITIVE.getMessage());
        }
        if ((action.equals("U") || action.equals("D")) && row.id() != null && !existing.containsKey(row.id())) {
            errors.add(ErrorMessages.ERR_ENTITY_NOT_FOUND.getMessage());
        }
        if (row.parameterId() != null && !params.containsKey(row.parameterId())) {
            errors.add("The specified parameter ID does not exist");
        }

        return errors;
    }

    private byte[] generateErrorFile(List<InventoryConfigurationBulkRow> rows, Map<Integer, List<String>> errors) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventory Configuration");
            Row header = sheet.createRow(0);
            String[] headers = {"Action (A/U/D)", "ID", "Product ID", "Parameter ID", "Count Needed", "Error Messages"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (InventoryConfigurationBulkRow row : rows) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.action());
                if (row.id() != null) r.createCell(1).setCellValue(row.id().doubleValue());
                r.createCell(2).setCellValue(row.productId() != null ? row.productId() : "");
                if (row.parameterId() != null) r.createCell(3).setCellValue(row.parameterId().doubleValue());
                if (row.countNeeded() != null) r.createCell(4).setCellValue(row.countNeeded().doubleValue());
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

    private Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return (int) Math.round(cell.getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }
}
