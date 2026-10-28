package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.InventoryConfigurationBulkRow;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.InventoryConfigurationRepository;
import com.medplus.qc_lims_backend.repository.MetricSpecificationRepository;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import com.medplus.qc_lims_backend.service.InventoryConfigurationServiceInterface;
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
import com.medplus.qc_lims_backend.dao.InventoryConfigurationDao;
import com.medplus.qc_lims_backend.domain.criteria.InventoryConfigSearchCriteria;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.service.external.ProductValidationService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InventoryConfigurationServiceImpl implements InventoryConfigurationServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {
            "ID", "Product ID", "Parameter ID", "Test Method ID",
            "Is Critical (Y/N)", "Count Needed", "Result Type (METRICS/TEXT)",
            "Specification Text", "Action (A/U/D)"
    };

    private final InventoryConfigurationRepository repository;
    private final TestParameterRepository parameterRepository;
    private final InventoryConfigurationDao inventoryConfigurationDao;
    private final MetricSpecificationRepository metricSpecificationRepository;
    private final ProductValidationService productValidationService;

    public InventoryConfigurationServiceImpl(InventoryConfigurationRepository repository,
                                             TestParameterRepository parameterRepository,
                                             InventoryConfigurationDao inventoryConfigurationDao,
                                             MetricSpecificationRepository metricSpecificationRepository,
                                             ProductValidationService productValidationService) {
        this.repository = repository;
        this.parameterRepository = parameterRepository;
        this.inventoryConfigurationDao = inventoryConfigurationDao;
        this.metricSpecificationRepository = metricSpecificationRepository;
        this.productValidationService = productValidationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InventoryConfiguration> getPagedInventoryConfig(InventoryConfigSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new InventoryConfigSearchCriteria();
        }
        long total = inventoryConfigurationDao.count(criteria);
        if (total == 0) {
            return PagedResponse.of(List.of(), 0, criteria.getPage(), criteria.getSize());
        }

        int offset = criteria.getOffset();
        List<Long> ids = inventoryConfigurationDao.getPageIds(criteria, offset, criteria.getSize());
        List<InventoryConfiguration> content = inventoryConfigurationDao.getEntitiesByIds(ids);

        return PagedResponse.of(content, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryConfiguration> findList(InventoryConfigSearchCriteria criteria, int maxResults) {
        if (criteria == null) {
            criteria = new InventoryConfigSearchCriteria();
        }
        return inventoryConfigurationDao.findList(criteria, maxResults);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<InventoryConfigurationBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        List<Long> ids = rows.stream().map(InventoryConfigurationBulkRow::id).filter(Objects::nonNull).distinct().toList();
        Map<Long, InventoryConfiguration> existingById = ids.isEmpty() ? Map.of() :
                repository.findAllById(ids).stream().collect(Collectors.toMap(InventoryConfiguration::getId, ic -> ic));

        List<Long> paramIds = rows.stream().map(InventoryConfigurationBulkRow::parameterId).filter(Objects::nonNull).distinct().toList();
        Map<Long, TestParameter> paramsById = paramIds.isEmpty() ? Map.of() :
                parameterRepository.findAllById(paramIds).stream().collect(Collectors.toMap(TestParameter::getId, tp -> tp));

        // Extract combination keys to query ONLY matching active records from DB via DAO,
        // and detect intra-file duplicate combination rows
        Set<String> comboKeys = new HashSet<>();
        Map<String, List<Integer>> rowsByCombo = new HashMap<>();
        Set<Long> candidateConfigIdsForOrphanCheck = new HashSet<>();

        for (InventoryConfigurationBulkRow r : rows) {
            String act = r.action() != null ? r.action().toUpperCase() : "";
            if (act.equals("A")) {
                if (r.productId() != null && !r.productId().isBlank() && r.parameterId() != null) {
                    String combo = r.productId().trim().toUpperCase() + ":" + r.parameterId();
                    comboKeys.add(combo);
                    rowsByCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(r.rowNumber());
                }
            } else if (act.equals("U") && r.id() != null && existingById.containsKey(r.id())) {
                InventoryConfiguration ex = existingById.get(r.id());
                if (ex.getStatus() == MasterStatus.ACTIVE) {
                    String prod = (r.productId() != null && !r.productId().isBlank())
                            ? r.productId().trim().toUpperCase()
                            : (ex.getProductId() != null ? ex.getProductId().trim().toUpperCase() : null);
                    Long pid = r.parameterId() != null
                            ? r.parameterId()
                            : (ex.getTestParameter() != null ? ex.getTestParameter().getId() : null);
                    if (prod != null && pid != null) {
                        String combo = prod + ":" + pid;
                        comboKeys.add(combo);
                        rowsByCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(r.rowNumber());
                    }
                }

                // Guardrail 2: check if changing from METRICS to TEXT
                if ("METRICS".equalsIgnoreCase(ex.getResultType())
                        && r.resultType() != null
                        && "TEXT".equalsIgnoreCase(r.resultType().trim())) {
                    candidateConfigIdsForOrphanCheck.add(r.id());
                }
            }
        }

        Set<String> duplicateComboKeys = rowsByCombo.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Targeted lookup for configs with active metric specs (Guardrail 2)
        Set<Long> configsWithActiveSpecs = candidateConfigIdsForOrphanCheck.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(metricSpecificationRepository.findConfigIdsWithActiveMetricSpecs(candidateConfigIdsForOrphanCheck, MasterStatus.ACTIVE));

        List<InventoryConfiguration> activeFromDb = inventoryConfigurationDao.findActiveByProductAndParamCombos(comboKeys);
        Map<String, List<InventoryConfiguration>> activeByCombo = activeFromDb.stream()
                .filter(ic -> ic.getProductId() != null && ic.getTestParameter() != null)
                .collect(Collectors.groupingBy(ic ->
                        ic.getProductId().trim().toUpperCase() + ":" + ic.getTestParameter().getId()));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, paramsById, activeByCombo, duplicateComboKeys, configsWithActiveSpecs);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        // Common generic bulk validator for Product IDs (DRY principle)
        productValidationService.validateProductIdsForBulkRows(
                rows,
                InventoryConfigurationBulkRow::productId,
                r -> ("A".equalsIgnoreCase(r.action()) || "U".equalsIgnoreCase(r.action()))
                        && r.productId() != null && !r.productId().trim().isEmpty(),
                errors
        );

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
                        r.createCell(col++).setCellValue(row.testMethodId() != null ? row.testMethodId() : "");
                        r.createCell(col++).setCellValue(row.isCritical() != null ? row.isCritical() : "");
                        if (row.countNeeded() != null) {
                            r.createCell(col++).setCellValue(row.countNeeded().doubleValue());
                        } else {
                            col++;
                        }
                        r.createCell(col++).setCellValue(row.resultType() != null ? row.resultType() : "");
                        r.createCell(col).setCellValue(row.specificationText() != null ? row.specificationText() : "");
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
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; rowNum++; continue; } // skip header row
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;

                // Action is at index 8 (last column)
                String action = ExcelUtils.getCellString(row, 8);
                String productId = ExcelUtils.getCellString(row, 1);
                String parameterIdStr = ExcelUtils.getCellString(row, 2);
                String testMethodId = ExcelUtils.getCellString(row, 3);
                String isCritical = ExcelUtils.getCellString(row, 4);
                String countStr = ExcelUtils.getCellString(row, 5);
                String resultType = ExcelUtils.getCellString(row, 6);
                String specificationText = ExcelUtils.getCellString(row, 7);

                if (action == null && productId == null && parameterIdStr == null
                        && testMethodId == null && isCritical == null && countStr == null
                        && resultType == null && specificationText == null) {
                    rowNum++;
                    continue;
                }

                Long id = ExcelUtils.getCellLong(row, 0);
                Long parameterId = ExcelUtils.getCellLong(row, 2);
                Integer countNeeded = ExcelUtils.getCellInt(row, 5);

                rows.add(new InventoryConfigurationBulkRow(
                        id,
                        action != null ? action.trim() : "",
                        productId,
                        parameterId,
                        testMethodId != null ? testMethodId.trim() : null,
                        isCritical != null ? isCritical.trim().toUpperCase() : null,
                        countNeeded,
                        resultType != null ? resultType.trim().toUpperCase() : null,
                        specificationText,
                        rowNum
                ));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(InventoryConfigurationBulkRow row,
                                     Map<Long, InventoryConfiguration> existing,
                                     Map<Long, TestParameter> params,
                                     Map<String, List<InventoryConfiguration>> activeByCombo,
                                     Set<String> duplicateComboKeys,
                                     Set<Long> configsWithActiveSpecs) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (!errors.isEmpty()) return errors;

        String action = row.action().toUpperCase();

        if (!action.equals("D")) {
            if (action.equals("A")) {
                if (row.productId() == null || row.productId().isBlank()) {
                    errors.add("Product ID is required");
                }
                if (row.parameterId() == null) {
                    errors.add("Parameter ID is required");
                } else if (!params.containsKey(row.parameterId())) {
                    errors.add(ErrorMessages.ERR_PARAM_NOT_FOUND.getMessage());
                }
                if (row.testMethodId() == null || row.testMethodId().isBlank()) {
                    errors.add(ErrorMessages.ERR_TEST_METHOD_REQUIRED.getMessage());
                } else if (row.testMethodId().length() > 50) {
                    errors.add("Test Method ID cannot exceed 50 characters");
                }
                if (row.isCritical() != null && !row.isCritical().matches("(?i)Y|N")) {
                    errors.add("Is Critical must be Y or N");
                }
                if (row.countNeeded() == null || row.countNeeded() <= 0) {
                    errors.add(ErrorMessages.ERR_COUNT_MUST_BE_POSITIVE.getMessage());
                }
                if (row.resultType() != null && !row.resultType().matches("(?i)METRICS|TEXT")) {
                    errors.add(ErrorMessages.ERR_INVALID_RESULT_TYPE.getMessage());
                }
                if (row.specificationText() != null && row.specificationText().length() > 300) {
                    errors.add("Specification Text cannot exceed 300 characters");
                }

                // Check active combination duplicate for 'A' (strictly one test method per product+param)
                if (row.productId() != null && !row.productId().isBlank() && row.parameterId() != null && params.containsKey(row.parameterId())) {
                    String comboKey = row.productId().trim().toUpperCase() + ":" + row.parameterId();
                    if (duplicateComboKeys.contains(comboKey)) {
                        errors.add(ErrorMessages.ERR_INVENTORY_DUPLICATE_IN_FILE.getMessage());
                    } else {
                        List<InventoryConfiguration> matching = activeByCombo.get(comboKey);
                        if (matching != null && !matching.isEmpty()) {
                            errors.add(ErrorMessages.ERR_INVENTORY_DUPLICATE.getMessage());
                        }
                    }
                }
            } else {
                // Update
                if (row.countNeeded() != null && row.countNeeded() <= 0) {
                    errors.add(ErrorMessages.ERR_COUNT_MUST_BE_POSITIVE.getMessage());
                }
                if (row.parameterId() != null && !params.containsKey(row.parameterId())) {
                    errors.add(ErrorMessages.ERR_PARAM_NOT_FOUND.getMessage());
                }
                if (row.testMethodId() != null && row.testMethodId().length() > 50) {
                    errors.add("Test Method ID cannot exceed 50 characters");
                }
                if (row.isCritical() != null && !row.isCritical().isBlank() && !row.isCritical().matches("(?i)Y|N")) {
                    errors.add("Is Critical must be Y or N");
                }
                if (row.resultType() != null && !row.resultType().isBlank() && !row.resultType().matches("(?i)METRICS|TEXT")) {
                    errors.add(ErrorMessages.ERR_INVALID_RESULT_TYPE.getMessage());
                }
                if (row.specificationText() != null && row.specificationText().length() > 300) {
                    errors.add("Specification Text cannot exceed 300 characters");
                }

                // Guardrail 2: Orphan protection when changing Result Type from METRICS to TEXT
                if (row.id() != null && configsWithActiveSpecs.contains(row.id())) {
                    errors.add(ErrorMessages.ERR_CANNOT_CHANGE_TO_TEXT.getMessage());
                }

                ExcelUtils.validateUpdateHasFields(row, errors,
                        "Product ID", row.productId(),
                        "Parameter ID", String.valueOf(row.parameterId()),
                        "Test Method ID", row.testMethodId(),
                        "Is Critical", row.isCritical(),
                        "Count Needed", String.valueOf(row.countNeeded()),
                        "Result Type", row.resultType(),
                        "Specification Text", row.specificationText());

                // Check active combination duplicate for 'U'
                if (row.id() != null && existing.containsKey(row.id()) && existing.get(row.id()).getStatus() == MasterStatus.ACTIVE) {
                    InventoryConfiguration ex = existing.get(row.id());
                    String effProd = (row.productId() != null && !row.productId().isBlank())
                            ? row.productId().trim().toUpperCase()
                            : (ex.getProductId() != null ? ex.getProductId().trim().toUpperCase() : null);
                    Long effParam = row.parameterId() != null
                            ? row.parameterId()
                            : (ex.getTestParameter() != null ? ex.getTestParameter().getId() : null);

                    if (effProd != null && effParam != null && (row.parameterId() == null || params.containsKey(row.parameterId()))) {
                        String comboKey = effProd + ":" + effParam;
                        if (duplicateComboKeys.contains(comboKey)) {
                            errors.add(ErrorMessages.ERR_INVENTORY_DUPLICATE_IN_FILE.getMessage());
                        } else {
                            List<InventoryConfiguration> matching = activeByCombo.get(comboKey);
                            if (matching != null && matching.stream().anyMatch(ic -> !ic.getId().equals(row.id()))) {
                                errors.add(ErrorMessages.ERR_INVENTORY_DUPLICATE.getMessage());
                            }
                        }
                    }
                }
            }
        }

        ExcelUtils.validateExists(row, existing, InventoryConfiguration::getStatus, errors, "Inventory Configuration");

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
                String isCrit = r.isCritical() != null && !r.isCritical().isBlank() ? r.isCritical().toUpperCase() : "N";
                String resType = r.resultType() != null && !r.resultType().isBlank() ? r.resultType().toUpperCase() : "TEXT";
                return InventoryConfiguration.builder()
                        .productId(r.productId().trim())
                        .testParameter(tp)
                        .testMethodId(r.testMethodId().trim())
                        .isCritical(isCrit)
                        .countNeeded(r.countNeeded())
                        .resultType(resType)
                        .specificationText(r.specificationText() != null ? r.specificationText().trim() : null)
                        .status(MasterStatus.ACTIVE)
                        .build();
            }).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<InventoryConfigurationBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<Long> updateIds = updates.stream().map(InventoryConfigurationBulkRow::id).toList();
            repository.logBeforeUpdate(updateIds, employeeId);

            List<InventoryConfiguration> toUpdate = updates.stream().map(r -> {
                InventoryConfiguration ic = existingById.get(r.id());
                if (r.productId() != null && !r.productId().isBlank()) {
                    ic.setProductId(r.productId().trim());
                }
                if (r.parameterId() != null) {
                    TestParameter tp = paramsById.get(r.parameterId());
                    ic.setTestParameter(tp);
                }
                if (r.testMethodId() != null && !r.testMethodId().isBlank()) {
                    ic.setTestMethodId(r.testMethodId().trim());
                }
                if (r.isCritical() != null && !r.isCritical().isBlank()) {
                    ic.setIsCritical(r.isCritical().toUpperCase().trim());
                }
                if (r.countNeeded() != null) {
                    ic.setCountNeeded(r.countNeeded());
                }
                if (r.resultType() != null && !r.resultType().isBlank()) {
                    ic.setResultType(r.resultType().toUpperCase().trim());
                }
                if (r.specificationText() != null) {
                    ic.setSpecificationText(r.specificationText().trim());
                }
                return ic;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<InventoryConfigurationBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(InventoryConfigurationBulkRow::id).toList();
            repository.logBeforeUpdate(ids, employeeId);
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
