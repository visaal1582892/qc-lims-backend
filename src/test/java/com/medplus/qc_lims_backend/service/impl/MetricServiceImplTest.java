package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.MetricDao;
import com.medplus.qc_lims_backend.domain.Metric;
import com.medplus.qc_lims_backend.domain.criteria.MetricSearchCriteria;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.dto.PagedResponse;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.repository.MetricRepository;
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
class MetricServiceImplTest {

    @Mock
    private MetricRepository repository;

    @Mock
    private MetricDao metricDao;

    private MetricServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MetricServiceImpl(repository, metricDao);
    }

    private byte[] createExcel(Object[][] rowsData) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Metrics");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Metric Name");
            header.createCell(2).setCellValue("Unit");
            header.createCell(3).setCellValue("Action (A/U/D)");

            int rIdx = 1;
            for (Object[] rowData : rowsData) {
                Row row = sheet.createRow(rIdx++);
                if (rowData[0] != null) row.createCell(0).setCellValue(((Number) rowData[0]).doubleValue());
                if (rowData[1] != null) row.createCell(1).setCellValue((String) rowData[1]);
                if (rowData[2] != null) row.createCell(2).setCellValue((String) rowData[2]);
                if (rowData[3] != null) row.createCell(3).setCellValue((String) rowData[3]);
            }
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testGetPagedMetrics_callsDao() {
        MetricSearchCriteria criteria = new MetricSearchCriteria();
        criteria.setMetricName("Assay");

        when(metricDao.count(criteria)).thenReturn(1L);
        when(metricDao.getPageIds(eq(criteria), anyInt(), anyInt())).thenReturn(List.of(1L));
        Metric metric = Metric.builder().id(1L).metricName("Assay").unit("%").status(MasterStatus.ACTIVE).build();
        when(metricDao.getEntitiesByIds(List.of(1L))).thenReturn(List.of(metric));

        PagedResponse<Metric> response = service.getPagedMetrics(criteria);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals("Assay", response.content().get(0).getMetricName());
        assertEquals("%", response.content().get(0).getUnit());
    }

    @Test
    void testUploadExcel_rejectsMissingMetricNameOnAdd() throws Exception {
        byte[] bytes = createExcel(new Object[][]{
                {null, "", "%", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains("Metric Name is required"));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void testUploadExcel_successOnValidAdd() throws Exception {
        byte[] bytes = createExcel(new Object[][]{
                {null, "Assay", "%", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        BulkUploadResponse response = service.uploadExcel(file, "EMP01");

        assertTrue(response.success());
        verify(repository, times(1)).saveAll(any());
    }
}
