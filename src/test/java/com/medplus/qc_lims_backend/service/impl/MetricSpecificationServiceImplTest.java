package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.MetricDao;
import com.medplus.qc_lims_backend.dao.MetricSpecificationDao;
import com.medplus.qc_lims_backend.domain.InventoryConfiguration;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.MetricSpecification;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.repository.InventoryConfigurationRepository;
import com.medplus.qc_lims_backend.repository.MetricSpecificationRepository;
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
class MetricSpecificationServiceImplTest {

    @Mock
    private MetricSpecificationRepository repository;

    @Mock
    private InventoryConfigurationRepository inventoryConfigurationRepository;

    @Mock
    private MetricDao metricDao;

    @Mock
    private MetricSpecificationDao metricSpecificationDao;

    private MetricSpecificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MetricSpecificationServiceImpl(
                repository,
                inventoryConfigurationRepository,
                metricDao,
                metricSpecificationDao
        );
    }

    private byte[] createExcel(Object[][] rowsData) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Metric Specifications");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Config ID");
            header.createCell(2).setCellValue("Metric ID");
            header.createCell(3).setCellValue("Lower Bound");
            header.createCell(4).setCellValue("Upper Bound");
            header.createCell(5).setCellValue("Action (A/U/D)");

            int rIdx = 1;
            for (Object[] rowData : rowsData) {
                Row row = sheet.createRow(rIdx++);
                if (rowData[0] != null) row.createCell(0).setCellValue(((Number) rowData[0]).doubleValue());
                if (rowData[1] != null) row.createCell(1).setCellValue(((Number) rowData[1]).doubleValue());
                if (rowData[2] != null) row.createCell(2).setCellValue(((Number) rowData[2]).doubleValue());
                if (rowData[3] != null) row.createCell(3).setCellValue(((Number) rowData[3]).doubleValue());
                if (rowData[4] != null) row.createCell(4).setCellValue(((Number) rowData[4]).doubleValue());
                if (rowData[5] != null) row.createCell(5).setCellValue((String) rowData[5]);
            }
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testGuardrail1_rejectsEmptyBounds() throws Exception {
        // Row with both lower and upper bound empty (null)
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, null, null, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("METRICS").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_METRIC_BOUNDS_EMPTY.getMessage()));
    }

    @Test
    void testGuardrail1_rejectsLowerBoundGreaterThanUpperBound() throws Exception {
        // Lower bound 105.00 > Upper bound 100.00
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, 105.00, 100.00, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("METRICS").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_BOUNDS_INVALID.getMessage()));
    }

    @Test
    void testRejectsConfigWithTextResultType() throws Exception {
        // Config ID has resultType "TEXT"
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, 95.00, 105.00, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("TEXT").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_CONFIG_NOT_METRICS.getMessage()));
    }

    @Test
    void testSuccessWhenValid() throws Exception {
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, 98.00, 102.00, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("METRICS").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertTrue(response.success());
        verify(repository, times(1)).saveAll(any());
    }

    @Test
    void testInFileDuplicate_flagsAllOccurrencesWithInFileMessage() throws Exception {
        // Upload 2 rows with the SAME (configId 10L, metricId 2L)
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, 95.00, 105.00, "A"},
                {null, 10L, 2L, 90.00, 110.00, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("METRICS").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2), "Row 2 should have errors");
        assertNotNull(response.errorRows().get(3), "Row 3 should have errors");
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE_IN_FILE.getMessage()));
        assertTrue(response.errorRows().get(3).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE_IN_FILE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(3).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE.getMessage()));
    }

    @Test
    void testDatabaseDuplicate_flagsWithDatabaseDuplicateMessage() throws Exception {
        // Upload 1 row that matches an active record in DB
        byte[] bytes = createExcel(new Object[][]{
                {null, 10L, 2L, 95.00, 105.00, "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        InventoryConfiguration config = InventoryConfiguration.builder()
                .id(10L).resultType("METRICS").status(MasterStatus.ACTIVE).build();
        Metric metric = Metric.builder().id(2L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();
        MetricSpecification dbSpec = MetricSpecification.builder()
                .id(501L)
                .inventoryConfiguration(config)
                .metric(metric)
                .status(MasterStatus.ACTIVE)
                .build();

        when(inventoryConfigurationRepository.findAllById(any())).thenReturn(List.of(config));
        when(metricDao.getEntitiesByIds(any())).thenReturn(List.of(metric));
        when(metricSpecificationDao.findActiveByConfigAndUnitCombos(any())).thenReturn(List.of(dbSpec));

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_METRIC_SPEC_DUPLICATE_IN_FILE.getMessage()));
    }
}
