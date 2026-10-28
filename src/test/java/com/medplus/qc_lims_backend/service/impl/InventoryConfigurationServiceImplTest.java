package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.InventoryConfigurationDao;
import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.repository.InventoryConfigurationRepository;
import com.medplus.qc_lims_backend.repository.MetricSpecificationRepository;
import com.medplus.qc_lims_backend.repository.TestParameterRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryConfigurationServiceImplTest {

    @Mock
    private InventoryConfigurationRepository repository;

    @Mock
    private TestParameterRepository parameterRepository;

    @Mock
    private InventoryConfigurationDao inventoryConfigurationDao;

    @Mock
    private MetricSpecificationRepository metricSpecificationRepository;

    @Mock
    private com.medplus.qc_lims_backend.service.external.ProductValidationService productValidationService;

    private InventoryConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryConfigurationServiceImpl(
                repository,
                parameterRepository,
                inventoryConfigurationDao,
                metricSpecificationRepository,
                productValidationService
        );
    }

    private byte[] createExcel(Object[][] rowsData) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Inventory Configuration");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Product ID");
            header.createCell(2).setCellValue("Parameter ID");
            header.createCell(3).setCellValue("Test Method ID");
            header.createCell(4).setCellValue("Is Critical (Y/N)");
            header.createCell(5).setCellValue("Count Needed");
            header.createCell(6).setCellValue("Result Type (METRICS/TEXT)");
            header.createCell(7).setCellValue("Specification Text");
            header.createCell(8).setCellValue("Action (A/U/D)");

            int rIdx = 1;
            for (Object[] rowData : rowsData) {
                Row row = sheet.createRow(rIdx++);
                if (rowData[0] != null) row.createCell(0).setCellValue(((Number) rowData[0]).doubleValue());
                if (rowData[1] != null) row.createCell(1).setCellValue((String) rowData[1]);
                if (rowData[2] != null) row.createCell(2).setCellValue(((Number) rowData[2]).doubleValue());
                if (rowData[3] != null) row.createCell(3).setCellValue((String) rowData[3]);
                if (rowData[4] != null) row.createCell(4).setCellValue((String) rowData[4]);
                if (rowData[5] != null) row.createCell(5).setCellValue(((Number) rowData[5]).doubleValue());
                if (rowData[6] != null) row.createCell(6).setCellValue((String) rowData[6]);
                if (rowData[7] != null) row.createCell(7).setCellValue((String) rowData[7]);
                if (rowData[8] != null) row.createCell(8).setCellValue((String) rowData[8]);
            }
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testGuardrail2_rejectsResultTypeChangeToTextWhenActiveMetricSpecsExist() throws Exception {
        // Updating existing config 101L (which has resultType METRICS) to TEXT
        byte[] bytes = createExcel(new Object[][]{
                {101L, "PROD01", 1L, "TM-01", "N", 5, "TEXT", "Colorless liquid", "U"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        InventoryConfiguration existingConfig = InventoryConfiguration.builder()
                .id(101L)
                .productId("PROD01")
                .testParameter(param)
                .testMethodId("TM-01")
                .isCritical("N")
                .countNeeded(5)
                .resultType("METRICS")
                .status(MasterStatus.ACTIVE)
                .build();

        when(repository.findAllById(any())).thenReturn(List.of(existingConfig));
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(inventoryConfigurationDao.findActiveByProductAndParamCombos(any())).thenReturn(List.of(existingConfig));
        // Mock that config 101L has active metric specifications!
        when(metricSpecificationRepository.findConfigIdsWithActiveMetricSpecs(any(), eq(MasterStatus.ACTIVE)))
                .thenReturn(List.of(101L));

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_CANNOT_CHANGE_TO_TEXT.getMessage()));
    }

    @Test
    void testGuardrail2_allowsResultTypeChangeToTextWhenNoActiveMetricSpecsExist() throws Exception {
        // Updating existing config 101L to TEXT when no active metric specs exist
        byte[] bytes = createExcel(new Object[][]{
                {101L, "PROD01", 1L, "TM-01", "N", 5, "TEXT", "Colorless liquid", "U"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        InventoryConfiguration existingConfig = InventoryConfiguration.builder()
                .id(101L)
                .productId("PROD01")
                .testParameter(param)
                .testMethodId("TM-01")
                .isCritical("N")
                .countNeeded(5)
                .resultType("METRICS")
                .status(MasterStatus.ACTIVE)
                .build();

        when(repository.findAllById(any())).thenReturn(List.of(existingConfig));
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(inventoryConfigurationDao.findActiveByProductAndParamCombos(any())).thenReturn(List.of(existingConfig));
        when(metricSpecificationRepository.findConfigIdsWithActiveMetricSpecs(any(), eq(MasterStatus.ACTIVE)))
                .thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertTrue(response.success());
        verify(repository, times(1)).saveAll(any());
        verify(repository, times(1)).logBeforeUpdate(any(), eq("EMP01"));
    }

    @Test
    void testBulkUpload_rejectsInvalidProductId() throws Exception {
        byte[] bytes = createExcel(new Object[][]{
                {null, "INVALID_PROD", 1L, "TM-01", "N", 5, "TEXT", "Colorless liquid", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(inventoryConfigurationDao.findActiveByProductAndParamCombos(any())).thenReturn(List.of());

        doAnswer(invocation -> {
            java.util.Map<Integer, List<String>> errorRows = invocation.getArgument(3);
            errorRows.computeIfAbsent(2, k -> new java.util.ArrayList<>())
                    .add(ErrorMessages.ERR_PRODUCT_NOT_FOUND_OR_INACTIVE.getMessage());
            return null;
        }).when(productValidationService).validateProductIdsForBulkRows(any(), any(), any(), any());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_PRODUCT_NOT_FOUND_OR_INACTIVE.getMessage()));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testInFileDuplicate_flagsAllOccurrencesWithInFileMessage() throws Exception {
        // Upload 2 rows with the SAME combination ("PROD01", parameter 1L)
        byte[] bytes = createExcel(new Object[][]{
                {null, "PROD01", 1L, "TM-01", "N", 5, "TEXT", "Spec 1", "A"},
                {null, "PROD01", 1L, "TM-02", "N", 10, "TEXT", "Spec 2", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(inventoryConfigurationDao.findActiveByProductAndParamCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        // Both row 2 and row 3 must be flagged with ERR_INVENTORY_DUPLICATE_IN_FILE
        assertNotNull(response.errorRows().get(2), "Row 2 should have errors");
        assertNotNull(response.errorRows().get(3), "Row 3 should have errors");
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE_IN_FILE.getMessage()));
        assertTrue(response.errorRows().get(3).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE_IN_FILE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(3).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE.getMessage()));
    }

    @Test
    void testDatabaseDuplicate_flagsWithDatabaseDuplicateMessage() throws Exception {
        // Upload 1 row that matches an active record in DB
        byte[] bytes = createExcel(new Object[][]{
                {null, "PROD01", 1L, "TM-01", "N", 5, "TEXT", "Spec 1", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        InventoryConfiguration dbConfig = InventoryConfiguration.builder()
                .id(999L)
                .productId("PROD01")
                .testParameter(param)
                .status(MasterStatus.ACTIVE)
                .build();

        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(inventoryConfigurationDao.findActiveByProductAndParamCombos(any())).thenReturn(List.of(dbConfig));

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_INVENTORY_DUPLICATE_IN_FILE.getMessage()));
    }
}
