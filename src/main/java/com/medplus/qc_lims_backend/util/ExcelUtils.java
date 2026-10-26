package com.medplus.qc_lims_backend.util;

import com.medplus.qc_lims_backend.dto.BulkRow;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared, reusable helpers for Apache POI Excel operations in bulk-upload workflows.
 *
 * DRY utility that extracts the duplicated cell-reading, workbook-iteration,
 * template-generation, and error-file-generation logic common to every master
 * bulk-upload service (MetricUnit, TestParameter, AnalystQualification, InventoryConfig).
 *
 * Each concrete service still provides its own domain-specific row type (BulkRow
 * subclass), validation rules, and entity-mapping logic — this class handles the
 * plumbing that is identical across all four.
 */
@UtilityClass
public final class ExcelUtils {

    // ------------------------------------------------------------------
    // 1. Cell value extractors
    // ------------------------------------------------------------------

    /**
     * Read a cell as a trimmed String. Returns {@code null} for missing/blank cells.
     */
    public static String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return cell.toString().trim();
    }

    /**
     * Read a cell as a Long. Returns {@code null} for missing or non-numeric cells.
     */
    public static Long getCellLong(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return Math.round(cell.getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read a cell as an Integer. Returns {@code null} for missing or non-numeric cells.
     */
    public static Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return (int) Math.round(cell.getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 2. Template & error-file generation
    // ------------------------------------------------------------------

    /**
     * Generate a blank Excel template with the given sheet name and column headers.
     *
     * @param sheetName  visible sheet label
     * @param headers    column header strings
     * @return           byte[] ready to be written to the response
     */
    public static byte[] createTemplate(String sheetName, String[] headers) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                wb.write(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate template: " + e.getMessage());
        }
    }

    /**
     * Generate an error-report Excel workbook from the parsed rows and the
     * validation-errors map. The last column is always "Error Messages".
     *
     * Column order follows the {@code headers} array. The writer lambda
     * populates {@code actionCol}, {@code idCol}, and any additional columns
     * at {@code startCol}.
     *
     * @param sheetName   visible sheet label
     * @param headers     column headers (the first entry is "ID", the last
     *                    entry is "Action (A/U/D)" — error report mirrors
     *                    this order; "Error Messages" is appended)
     * @param rows        parsed BulkRow instances
     * @param errors      rowNumber → error list mapping
     * @param cellWriters lambda that writes domain-specific cells starting
     *                    at {@code startCol} (which is after ID and Action)
     */
    public static byte[] createErrorReport(
            String sheetName,
            String[] headers,
            List<? extends BulkRow> rows,
            Map<Integer, List<String>> errors,
            RowCellWriter cellWriter) {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            int numCols = headers.length + 1; // +1 for "Error Messages"
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            headerRow.createCell(numCols - 1).setCellValue("Error Messages");

            // headers[0] = ID, headers[last] = Action; data columns are in between
            int idCol = 0;
            int actionCol = headers.length - 1;

            int rowNum = 1;
            for (BulkRow bulkRow : rows) {
                Row r = sheet.createRow(rowNum++);
                // ID column (first)
                if (bulkRow.id() != null) {
                    r.createCell(idCol).setCellValue(bulkRow.id().doubleValue());
                }
                // Action column (last, just before Error Messages)
                r.createCell(actionCol).setCellValue(bulkRow.action());
                // Domain-specific columns (between ID and Action)
                cellWriter.writeCells(r, bulkRow, 1);

                // Error messages column
                List<String> rowErrors = errors.get(bulkRow.rowNumber());
                if (rowErrors != null) {
                    r.createCell(numCols - 1).setCellValue(String.join("; ", rowErrors));
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

    /**
     * Functional interface for writing domain-specific cells in the error report.
     * Called after ID (col 0) and Action (col headers.length-1) are already written.
     *
     * @param row       the POI Row being written
     * @param bulkRow   the parsed BulkRow for this row
     * @param startCol  the column index to start writing domain-specific cells (after ID)
     */
    @FunctionalInterface
    public interface RowCellWriter {
        void writeCells(Row row, BulkRow bulkRow, int startCol);
    }

    // ------------------------------------------------------------------
    // 3. Row-level validators (shared rules)
    // ------------------------------------------------------------------

    /**
     * Validate the common Action/ID rules for any BulkRow.
     * Adds errors to the provided list.
     */
    public static void validateActionAndId(BulkRow row, List<String> errors) {
        String action = row.action().toUpperCase();

        // Action must be A, U, or D
        if (!action.matches("[AUD]")) {
            errors.add("Action must be A (Add), U (Update), or D (Deactivate)");
            return;
        }

        // Add must not have an ID
        if (action.equals("A") && row.id() != null) {
            errors.add("ID is not allowed for Add action");
        }

        // Update/Deactivate must have an ID
        if ((action.equals("U") || action.equals("D")) && row.id() == null) {
            errors.add("ID is required for Update/Deactivate action");
        }
    }

    /**
     * Check if the entity exists in the provided map (for U/D actions).
     * Adds an error if not found.
     */
    public static <E> void validateExists(
            BulkRow row,
            Map<Long, E> existingById,
            List<String> errors,
            String entityLabel) {

        String action = row.action().toUpperCase();
        if ((action.equals("U") || action.equals("D")) && row.id() != null) {
            if (!existingById.containsKey(row.id())) {
                errors.add(entityLabel + " with ID " + row.id() + " not found");
            }
        }
    }

    // ------------------------------------------------------------------
    // 4. Row iteration helpers
    // ------------------------------------------------------------------

    /**
     * Check if the given row exceeds the physical sheet bounds
     * (used to break the loop and avoid processing empty trailing rows).
     */
    public static boolean isLastRow(int rowNum, Sheet sheet) {
        return rowNum > sheet.getPhysicalNumberOfRows();
    }

    /**
     * Check if a parsed row is a "blank" row (action is null or blank).
     */
    public static boolean isBlankRow(BulkRow row) {
        return row == null || row.action() == null || row.action().isBlank();
    }

    // ------------------------------------------------------------------
    // 5. File validation
    // ------------------------------------------------------------------

    /**
     * Validate the uploaded MultipartFile is present and non-empty.
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an Excel file to upload.");
        }
    }

    /**
     * Validate the number of parsed rows does not exceed the maximum.
     */
    public static void validateRowCount(List<? extends BulkRow> rows, int maxRows) {
        if (rows.isEmpty()) {
            throw new BadRequestException("No valid data rows found in the uploaded file.");
        }
        if (rows.size() > maxRows) {
            throw new BadRequestException(
                    "File contains too many rows (max " + maxRows + ").");
        }
    }

    // ------------------------------------------------------------------
    // 6. Entity status helper
    // ------------------------------------------------------------------

    /**
     * Format the human-readable label for a MasterStatus enum value.
     */
    public static String statusLabel(MasterStatus status) {
        return status == MasterStatus.ACTIVE ? "Active" : "Inactive";
    }
}
