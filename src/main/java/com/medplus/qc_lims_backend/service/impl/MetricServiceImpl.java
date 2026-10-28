package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.MetricDao;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.criteria.MetricSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.MetricBulkRow;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import com.medplus.qc_lims_backend.repository.MetricRepository;
import com.medplus.qc_lims_backend.service.MetricServiceInterface;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricServiceImpl implements MetricServiceInterface {

    private static final int MAX_ROWS = 1000;
    private static final String[] HEADERS = {"ID", "Metric Name", "Unit", "Action (A/U/D)"};

    private final MetricRepository repository;
    private final MetricDao metricDao;

    public MetricServiceImpl(MetricRepository repository, MetricDao metricDao) {
        this.repository = repository;
        this.metricDao = metricDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Metric> getPagedMetrics(MetricSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new MetricSearchCriteria();
        }
        long total = metricDao.count(criteria);
        if (total == 0) {
            return PagedResponse.of(List.of(), 0, criteria.getPage(), criteria.getSize());
        }

        int offset = criteria.getOffset();
        List<Long> ids = metricDao.getPageIds(criteria, offset, criteria.getSize());
        List<Metric> content = metricDao.getEntitiesByIds(ids);

        return PagedResponse.of(content, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Metric> findList(MetricSearchCriteria criteria, int maxResults) {
        if (criteria == null) {
            criteria = new MetricSearchCriteria();
        }
        return metricDao.findList(criteria, maxResults);
    }

    @Override
    @Transactional
    public BulkUploadResponse uploadExcel(MultipartFile file, String employeeId) {
        ExcelUtils.validateFile(file);

        List<MetricBulkRow> rows = parseFile(file);
        ExcelUtils.validateRowCount(rows, MAX_ROWS);

        List<Long> ids = rows.stream().map(MetricBulkRow::id).filter(Objects::nonNull).distinct().toList();
        Map<Long, Metric> existingById = ids.isEmpty() ? Map.of() :
                repository.findAllById(ids).stream().collect(Collectors.toMap(Metric::getId, m -> m));

        Map<Integer, List<String>> errors = new ConcurrentHashMap<>();
        rows.parallelStream().forEach(row -> {
            List<String> rowErrors = validateRow(row, existingById);
            if (!rowErrors.isEmpty()) {
                errors.put(row.rowNumber(), rowErrors);
            }
        });

        if (!errors.isEmpty()) {
            byte[] errorFile = ExcelUtils.createErrorReport(
                    "Metrics", HEADERS, rows, errors, (r, bulkRow, col) -> {
                        MetricBulkRow row = (MetricBulkRow) bulkRow;
                        r.createCell(col++).setCellValue(row.metricName() != null ? row.metricName() : "");
                        r.createCell(col).setCellValue(row.unit() != null ? row.unit() : "");
                    });
            return BulkUploadResponse.validationFailureWithFile(errors, errorFile);
        }

        return executeBulk(rows, existingById, employeeId);
    }

    @Override
    public byte[] downloadTemplate() {
        return ExcelUtils.createTemplate("Metrics", HEADERS);
    }

    // ---- Domain-specific logic ----

    private List<MetricBulkRow> parseFile(MultipartFile file) {
        List<MetricBulkRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 1;
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; rowNum++; continue; } // skip header row
                if (ExcelUtils.isLastRow(rowNum, sheet)) break;
                // Skip completely blank rows
                String action = ExcelUtils.getCellString(row, 3);
                String metricName = ExcelUtils.getCellString(row, 1);
                String unit = ExcelUtils.getCellString(row, 2);
                if (action == null && metricName == null && unit == null) {
                    rowNum++;
                    continue;
                }
                Long id = ExcelUtils.getCellLong(row, 0);
                rows.add(new MetricBulkRow(id, action != null ? action.trim() : "", metricName, unit, rowNum));
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read Excel file: " + e.getMessage());
        }
        return rows;
    }

    private List<String> validateRow(MetricBulkRow row, Map<Long, Metric> existing) {
        List<String> errors = new ArrayList<>();
        ExcelUtils.validateActionAndId(row, errors);
        if (!errors.isEmpty()) return errors;

        String action = row.action().toUpperCase();

        // For Deactivate, only ID + Action are needed — no field validation
        if (!action.equals("D")) {
            // For Add: metricName required
            // For Update: at least one field must be provided
            if (action.equals("A")) {
                if (row.metricName() == null || row.metricName().isBlank()) {
                    errors.add("Metric Name is required");
                }
            } else {
                ExcelUtils.validateUpdateHasFields(row, errors,
                        "Metric Name", row.metricName(), "Unit", row.unit());
            }
        }

        ExcelUtils.validateExists(row, existing, Metric::getStatus, errors, "Metric");

        return errors;
    }

    private BulkUploadResponse executeBulk(
            List<MetricBulkRow> rows,
            Map<Long, Metric> existingById,
            String employeeId) {

        Map<String, List<MetricBulkRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> r.action().toUpperCase()));

        int added = 0, updated = 0, deactivated = 0;

        List<MetricBulkRow> adds = grouped.getOrDefault("A", List.of());
        if (!adds.isEmpty()) {
            List<Metric> toSave = adds.stream().map(r ->
                    Metric.builder()
                            .metricName(r.metricName())
                            .unit(r.unit())
                            .status(MasterStatus.ACTIVE)
                            .build()
            ).toList();
            repository.saveAll(toSave);
            added = toSave.size();
        }

        List<MetricBulkRow> updates = grouped.getOrDefault("U", List.of());
        if (!updates.isEmpty()) {
            List<Long> updateIds = updates.stream().map(MetricBulkRow::id).toList();
            repository.logBeforeUpdate(updateIds, employeeId);

            List<Metric> toUpdate = updates.stream().map(r -> {
                Metric existing = existingById.get(r.id());
                if (r.metricName() != null && !r.metricName().isBlank()) {
                    existing.setMetricName(r.metricName());
                }
                if (r.unit() != null && !r.unit().isBlank()) {
                    existing.setUnit(r.unit());
                }
                return existing;
            }).toList();
            repository.saveAll(toUpdate);
            updated = toUpdate.size();
        }

        List<MetricBulkRow> deletes = grouped.getOrDefault("D", List.of());
        if (!deletes.isEmpty()) {
            List<Long> ids = deletes.stream().map(MetricBulkRow::id).toList();
            repository.logBeforeUpdate(ids, employeeId);
            repository.bulkDeactivate(ids, MasterStatus.INACTIVE, employeeId);
            deactivated = ids.size();
        }

        String summary = String.format("Success — A:%d U:%d D:%d", added, updated, deactivated);
        log.info("Metric bulk upload: {}", summary);
        return BulkUploadResponse.success(summary);
    }
}
