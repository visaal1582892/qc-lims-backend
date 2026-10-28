package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.MetricDao;
import com.medplus.qc_lims_backend.dao.MetricSpecificationDao;
import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.domain.criteria.MetricSpecificationSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.MetricSpecificationBulkRow;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.InventoryConfigurationRepository;
import com.medplus.qc_lims_backend.repository.MetricSpecificationRepository;
import com.medplus.qc_lims_backend.service.MetricSpecificationServiceInterface;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricSpecificationServiceImpl implements MetricSpecificationServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Config ID", "Metric ID", "Lower Bound", "Upper Bound", "Action (A/U/D)"};

    private final MetricSpecificationRepository repository;
    private final InventoryConfigurationRepository inventoryConfigurationRepository;
    private final MetricDao metricDao;
    private final MetricSpecificationDao metricSpecificationDao;

    public MetricSpecificationServiceImpl(MetricSpecificationRepository repository,
                                          InventoryConfigurationRepository inventoryConfigurationRepository,
                                          MetricDao metricDao,
                                          MetricSpecificationDao metricSpecificationDao) {
        this.repository = repository;
        this.inventoryConfigurationRepository = inventoryConfigurationRepository;
        this.metricDao = metricDao;
        this.metricSpecificationDao = metricSpecificationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MetricSpecification> getPagedMetricSpecifications(MetricSpecificationSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new MetricSpecificationSearchCriteria();
        }
        long total = metricSpecificationDao.count(criteria);
        if (total == 0) {
            return PagedResponse.of(List.of(), 0, criteria.getPage(), criteria.getSize());
        }

        int offset = criteria.getOffset();
        List<Long> ids = metricSpecificationDao.getPageIds(criteria, offset, criteria.getSize());
        List<MetricSpecification> content = metricSpecificationDao.getEntitiesByIds(ids);

        return PagedResponse.of(content, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricSpecification> findList(MetricSpecificationSearchCriteria criteria, int maxResults) {
        if (criteria == null) {
            criteria = new MetricSpecificationSearchCriteria();
        }
        return metricSpecificationDao.findList(criteria, maxResults);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<MetricSpecificationBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        List<Long> ids = rows.stream().map(MetricSpecificationBulkRow::id).filter(Objects::nonNull).distinct().toList();
        Map<Long, MetricSpecification> existingById = ids.isEmpty() ? Map.of() :
                repository.findAllById(ids).stream().collect(Collectors.toMap(MetricSpecification::getId, ms -> ms));

        List<Long> configIds = rows.stream().map(MetricSpecificationBulkRow::configId).filter(Objects::nonNull).distinct().toList();
        Map<Long, InventoryConfiguration> configsById = configIds.isEmpty() ? Map.of() :
                inventoryConfigurationRepository.findAllById(configIds).stream().collect(Collectors.toMap(InventoryConfiguration::getId, ic -> ic));

        // Memory-Safe bulk validation via DAO (DATA_ACCESS_ARCHITECTURE.md)
        List<Long> metricIds = rows.stream().map(MetricSpecificationBulkRow::metricId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Metric> metricsById = metricIds.isEmpty() ? Map.of() :
                metricDao.getEntitiesByIds(metricIds).stream().collect(Collectors.toMap(Metric::getId, m -> m));

        // Extract combination keys to query ONLY matching active records from DB via DAO,
        // and detect intra-file duplicate combination rows
        Set<String> comboKeys = new HashSet<>();
        Map<String, List<Integer>> rowsByCombo = new HashMap<>();

        for (MetricSpecificationBulkRow r : rows) {
            String act = r.action() != null ? r.action().toUpperCase() : "";
            if (act.equals("A")) {
                if (r.configId() != null && r.metricId() != null) {
                    String combo = r.configId() + ":" + r.metricId();
                    comboKeys.add(combo);
                    rowsByCombo.computeIfAbsent(combo, k -> new ArrayList<>()).add(r.rowNumber());
                }
            } else if (act.equals("U") && r.id() != null && existingById.containsKey(r.id())) {
                MetricSpecification ex = existingById.get(r.id());
                if (ex.getStatus() == MasterStatus.ACTIVE) {
                    Long cid = r.configId() != null ? r.configId() : (ex.getInventoryConfiguration() != null ? ex.getInventoryConfiguration().getId() : null);
                    Long uid = r.metricId() != null ? r.metricId() : (ex.getMetric() != null ? ex.getMetric().getId() : null);
                    if (cid != null && uid != null) {
                        String combo = cid + ":" + uid;
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

        List<MetricSpecification> activeFromDb = metricSpecificationDao.findActiveByConfigAndUnitCombos(comboKeys);
        Map<String, List<MetricSpecification>> activeByCombo = activeFromDb.stream()
                .filter(ms -> ms.getInventoryConfiguration() != null && ms.getMetric() != null)
                .collect(Collectors.groupingBy(ms ->
                        ms.getInventoryConfiguration().getId() + ":" + ms.getMetric().getId()));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();

        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById, configsById, metricsById, activeByCombo, duplicateComboKeys);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = ExcelUtils.createErrorReport(
                    "Metric Specifications",
                    Arrays.copyOf(HEADERS, HEADERS.length),
                    rows, errors, (r, bulkRow, col) -> {
                        MetricSpecificationBulkRow row = (MetricSpecificationBulkRow) bulkRow;
                        if (row.configId() != null) {
                            r.createCell(col++).setCellValue(row.configId().doubleValue());
                        } else {
                            col++;
                        }
                        if (row.metricId() != null) {
                            r.createCell(col++).setCellValue(row.metricId().doubleValue());
                        } else {
                            col++;
                        }
                        if (row.lowerBound() != null) {
                            r.createCell(col++).setCellValue(row.lowerBound().doubleValue());
                        } else {
                            col++;
                        }
                        if (row.upperBound() != null) {
                            r.createCell(col).setCellValue(row.upperBound().doubleValue());
                        }
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, configsById, metricsById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Metric Specifications", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<MetricSpecificationBulkRow> parseFile(MultipartFile file) {
        List<MetricSpecificationBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; rowNum++; continue; } // skip header row
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;

                // Action is at index 5 (last column)
                String action = ExcelUtils.getCellString(row, 5);
                Long id = ExcelUtils.getCellLong(row, 0);
                Long configId = ExcelUtils.getCellLong(row, 1);
                Long metricId = ExcelUtils.getCellLong(row, 2);
                BigDecimal lowerBound = ExcelUtils.getCellDecimal(row, 3);
                BigDecimal upperBound = ExcelUtils.getCellDecimal(row, 4);

                if (action == null && id == null && configId == null && metricId == null
                        && lowerBound == null && upperBound == null) {
                    rowNum++;
                    continue;
                }

                rows.add(new MetricSpecificationBulkRow(
                        id,
                        action != null ? action.trim() : "",
                        configId,
                        metricId,
                        lowerBound,
                        upperBound,
                        rowNum
                ));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(MetricSpecificationBulkRow row,
                                     Map<Long, MetricSpecification> existing,
                                     Map<Long, InventoryConfiguration> configs,
                                     Map<Long, Metric> metrics,
                                     Map<String, List<MetricSpecification>> activeByCombo,
                                     Set<String> duplicateComboKeys) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (!errors.isEmpty()) return errors;

        String action = row.action().toUpperCase();

        if (!action.equals("D")) {
            if (action.equals("A")) {
                // Config ID validation
                if (row.configId() == null) {
                    errors.add("Config ID is required");
                } else if (!configs.containsKey(row.configId())) {
                    errors.add(ErrorMessages.ERR_CONFIG_NOT_FOUND.getMessage());
                } else {
                    InventoryConfiguration cfg = configs.get(row.configId());
                    if (cfg.getStatus() != MasterStatus.ACTIVE) {
                        errors.add(ErrorMessages.ERR_INACTIVE_RECORD.getMessage());
                    } else if (!"METRICS".equalsIgnoreCase(cfg.getResultType())) {
                        errors.add(ErrorMessages.ERR_CONFIG_NOT_METRICS.getMessage());
                    }
                }

                // Metric ID validation
                if (row.metricId() == null) {
                    errors.add("Metric ID is required");
                } else if (!metrics.containsKey(row.metricId())) {
                    errors.add(ErrorMessages.ERR_INVALID_METRIC.getMessage());
                } else if (metrics.get(row.metricId()).getStatus() != MasterStatus.ACTIVE) {
                    errors.add(ErrorMessages.ERR_INACTIVE_RECORD.getMessage());
                }

                // Guardrail 1: Bounds validation (No empty bounds)
                if (row.lowerBound() == null && row.upperBound() == null) {
                    errors.add(ErrorMessages.ERR_METRIC_BOUNDS_EMPTY.getMessage());
                } else if (row.lowerBound() != null && row.upperBound() != null) {
                    if (row.lowerBound().compareTo(row.upperBound()) > 0) {
                        errors.add(ErrorMessages.ERR_BOUNDS_INVALID.getMessage());
                    }
                }

                // Duplicate active unit check on (config_id, metric_id)
                if (row.configId() != null && row.metricId() != null && configs.containsKey(row.configId()) && metrics.containsKey(row.metricId())) {
                    String comboKey = row.configId() + ":" + row.metricId();
                    if (duplicateComboKeys.contains(comboKey)) {
                        errors.add(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE_IN_FILE.getMessage());
                    } else {
                        List<MetricSpecification> matching = activeByCombo.get(comboKey);
                        if (matching != null && !matching.isEmpty()) {
                            errors.add(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE.getMessage());
                        }
                    }
                }
            } else {
                // Update
                if (row.configId() != null) {
                    if (!configs.containsKey(row.configId())) {
                        errors.add(ErrorMessages.ERR_CONFIG_NOT_FOUND.getMessage());
                    } else {
                        InventoryConfiguration cfg = configs.get(row.configId());
                        if (cfg.getStatus() != MasterStatus.ACTIVE) {
                            errors.add(ErrorMessages.ERR_INACTIVE_RECORD.getMessage());
                        } else if (!"METRICS".equalsIgnoreCase(cfg.getResultType())) {
                            errors.add(ErrorMessages.ERR_CONFIG_NOT_METRICS.getMessage());
                        }
                    }
                }

                if (row.metricId() != null) {
                    if (!metrics.containsKey(row.metricId())) {
                        errors.add(ErrorMessages.ERR_INVALID_METRIC.getMessage());
                    } else if (metrics.get(row.metricId()).getStatus() != MasterStatus.ACTIVE) {
                        errors.add(ErrorMessages.ERR_INACTIVE_RECORD.getMessage());
                    }
                }

                // Bounds validation for Update
                if (row.id() != null && existing.containsKey(row.id())) {
                    MetricSpecification ex = existing.get(row.id());
                    BigDecimal effLower = row.lowerBound() != null ? row.lowerBound() : ex.getLowerBound();
                    BigDecimal effUpper = row.upperBound() != null ? row.upperBound() : ex.getUpperBound();

                    if (effLower == null && effUpper == null) {
                        errors.add(ErrorMessages.ERR_METRIC_BOUNDS_EMPTY.getMessage());
                    } else if (effLower != null && effUpper != null && effLower.compareTo(effUpper) > 0) {
                        errors.add(ErrorMessages.ERR_BOUNDS_INVALID.getMessage());
                    }

                    // Duplicate active combo check for Update
                    Long effConfig = row.configId() != null ? row.configId() : (ex.getInventoryConfiguration() != null ? ex.getInventoryConfiguration().getId() : null);
                    Long effMetric = row.metricId() != null ? row.metricId() : (ex.getMetric() != null ? ex.getMetric().getId() : null);

                    if (effConfig != null && effMetric != null) {
                        String comboKey = effConfig + ":" + effMetric;
                        if (duplicateComboKeys.contains(comboKey)) {
                            errors.add(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE_IN_FILE.getMessage());
                        } else {
                            List<MetricSpecification> matching = activeByCombo.get(comboKey);
                            if (matching != null && matching.stream().anyMatch(ms -> !ms.getId().equals(row.id()))) {
                                errors.add(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE.getMessage());
                            }
                        }
                    }
                }

                ExcelUtils.validateUpdateHasFields(row, errors,
                        "Config ID", String.valueOf(row.configId()),
                        "Metric ID", String.valueOf(row.metricId()),
                        "Lower Bound", String.valueOf(row.lowerBound()),
                        "Upper Bound", String.valueOf(row.upperBound()));
            }
        }

        ExcelUtils.validateExists(row, existing, MetricSpecification::getStatus, errors, "Metric Specification");

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<MetricSpecificationBulkRow> rows,
            Map<Long, MetricSpecification> existingById,
            Map<Long, InventoryConfiguration> configsById,
            Map<Long, Metric> metricsById,
            String employeeId) {

        Map<String, List<MetricSpecificationBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<MetricSpecificationBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<MetricSpecification> toSave = adds.stream().map(r -> {
                InventoryConfiguration cfg = configsById.get(r.configId());
                Metric m = metricsById.get(r.metricId());
                return MetricSpecification.builder()
                        .inventoryConfiguration(cfg)
                        .metric(m)
                        .lowerBound(r.lowerBound())
                        .upperBound(r.upperBound())
                        .status(MasterStatus.ACTIVE)
                        .build();
            }).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<MetricSpecificationBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<Long> updateIds = updates.stream().map(MetricSpecificationBulkRow::id).toList();
            repository.logBeforeUpdate(updateIds, employeeId);

            List<MetricSpecification> toUpdate = updates.stream().map(r -> {
                MetricSpecification ms = existingById.get(r.id());
                if (r.configId() != null) {
                    ms.setInventoryConfiguration(configsById.get(r.configId()));
                }
                if (r.metricId() != null) {
                    ms.setMetric(metricsById.get(r.metricId()));
                }
                if (r.lowerBound() != null) {
                    ms.setLowerBound(r.lowerBound());
                }
                if (r.upperBound() != null) {
                    ms.setUpperBound(r.upperBound());
                }
                return ms;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<MetricSpecificationBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(MetricSpecificationBulkRow::id).toList();
            repository.logBeforeUpdate(ids, employeeId);
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        return BulkUploadResponse.success(String.format("Success — A:%d U:%d D:%d", added, updated, deactivated));
    }
}
