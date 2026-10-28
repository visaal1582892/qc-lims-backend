package com.medplus.qc_lims_backend.service.impl;

import com.medplus.qc_lims_backend.dao.AnalystQualificationDao;
import com.medplus.qc_lims_backend.domain.AnalystQualification;
import com.medplus.qc_lims_backend.domain.TestParameter;
import com.medplus.qc_lims_backend.dto.BulkUploadResponse;
import com.medplus.qc_lims_backend.enums.ErrorMessages;
import com.medplus.qc_lims_backend.enums.MasterStatus;
import com.medplus.qc_lims_backend.repository.AnalystQualificationRepository;
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
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalystQualificationServiceImplTest {

    @Mock
    private AnalystQualificationRepository repository;

    @Mock
    private TestParameterRepository parameterRepository;

    @Mock
    private AnalystQualificationDao analystQualificationDao;

    private AnalystQualificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalystQualificationServiceImpl(
                repository,
                parameterRepository,
                analystQualificationDao
        );
    }

    private byte[] createExcel(Object[][] rowsData) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Analyst Qualifications");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Employee ID");
            header.createCell(2).setCellValue("Parameter ID");
            header.createCell(3).setCellValue("Qualified On (YYYY-MM-DD)");
            header.createCell(4).setCellValue("Action (A/U/D)");

            int rIdx = 1;
            for (Object[] rowData : rowsData) {
                Row row = sheet.createRow(rIdx++);
                if (rowData[0] != null) row.createCell(0).setCellValue(((Number) rowData[0]).doubleValue());
                if (rowData[1] != null) row.createCell(1).setCellValue((String) rowData[1]);
                if (rowData[2] != null) row.createCell(2).setCellValue(((Number) rowData[2]).doubleValue());
                if (rowData[3] != null) row.createCell(3).setCellValue((String) rowData[3]);
                if (rowData[4] != null) row.createCell(4).setCellValue((String) rowData[4]);
            }
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testInFileDuplicate_flagsAllOccurrencesWithInFileMessage() throws Exception {
        // Upload 2 rows with the SAME combination ("EMP100", parameter 1L)
        byte[] bytes = createExcel(new Object[][]{
                {null, "EMP100", 1L, "2026-01-15", "A"},
                {null, "EMP100", 1L, "2026-02-20", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(analystQualificationDao.findActiveByEmployeeAndParamCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "ADMIN");

        assertFalse(response.success());
        // Both row 2 and row 3 must be flagged with ERR_QUALIFICATION_DUPLICATE_IN_FILE
        assertNotNull(response.errorRows().get(2), "Row 2 should have errors");
        assertNotNull(response.errorRows().get(3), "Row 3 should have errors");
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE_IN_FILE.getMessage()));
        assertTrue(response.errorRows().get(3).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE_IN_FILE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(3).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE.getMessage()));
    }

    @Test
    void testDatabaseDuplicate_flagsWithDatabaseDuplicateMessage() throws Exception {
        // Upload 1 row that matches an active record in DB
        byte[] bytes = createExcel(new Object[][]{
                {null, "EMP100", 1L, "2026-01-15", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        AnalystQualification dbQual = AnalystQualification.builder()
                .id(888L)
                .employeeId("EMP100")
                .testParameter(param)
                .qualifiedOn(LocalDate.of(2025, 1, 1))
                .status(MasterStatus.ACTIVE)
                .build();

        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(analystQualificationDao.findActiveByEmployeeAndParamCombos(any())).thenReturn(List.of(dbQual));

        BulkUploadResponse response = service.uploadExcel(file, "ADMIN");

        assertFalse(response.success());
        assertNotNull(response.errorRows().get(2));
        assertTrue(response.errorRows().get(2).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE.getMessage()));
        assertFalse(response.errorRows().get(2).contains(ErrorMessages.ERR_QUALIFICATION_DUPLICATE_IN_FILE.getMessage()));
    }

    @Test
    void testSuccessWhenValid() throws Exception {
        byte[] bytes = createExcel(new Object[][]{
                {null, "EMP100", 1L, "2026-01-15", "A"}
        });
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        TestParameter param = TestParameter.builder().id(1L).name("Assay").status(MasterStatus.ACTIVE).build();
        when(parameterRepository.findAllById(any())).thenReturn(List.of(param));
        when(analystQualificationDao.findActiveByEmployeeAndParamCombos(any())).thenReturn(List.of());

        BulkUploadResponse response = service.uploadExcel(file, "ADMIN");

        assertTrue(response.success());
        verify(repository, times(1)).saveAll(any());
    }
}
