package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.DocumentResponse;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.service.DocumentManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T-19: DocumentManagementController — verifies thin controller delegates to service
 * and returns correct HTTP status codes and ApiResponse envelopes.
 */
class DocumentManagementControllerTest {

    private final DocumentManagementService service = mock(DocumentManagementService.class);
    private final DocumentManagementController controller = new DocumentManagementController(service);

    private DocumentResponse makeResponse() {
        return new DocumentResponse(
                1L, "ROA", "AR-2026-001", 1L,
                "roa.pdf", 1024L, "application/pdf",
                Instant.now(), 1L);
    }

    private MultipartFile mockPdfFile() throws Exception {
        return new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());
    }

    // --- POST upload-roa (200 OK — controller uses ResponseEntity.ok) ---

    @Test
    void uploadRoaReturns201() throws Exception {
        when(service.uploadRoa(anyLong(), any(MultipartFile.class))).thenReturn(makeResponse());

        ResponseEntity<ApiResponse<DocumentResponse>> response = controller.uploadRoa(mockPdfFile(), 1L);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("ROA", response.getBody().data().documentType());
    }

    // --- POST upload-coa (200 OK — controller uses ResponseEntity.ok) ---

    @Test
    void uploadCoaReturns201() throws Exception {
        DocumentResponse coa = new DocumentResponse(
                2L, "COA", "AR-2026-001", null,
                "coa.pdf", 2048L, "application/pdf",
                Instant.now(), 1L);

        when(service.uploadCoa(anyString(), any(MultipartFile.class))).thenReturn(coa);

        ResponseEntity<ApiResponse<DocumentResponse>> response = controller.uploadCoa(mockPdfFile(), "AR-2026-001");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("COA", response.getBody().data().documentType());
    }

    // --- GET getByAr (200 OK) ---

    @Test
    void getDocumentsByArReturns200() {
        when(service.getDocuments("AR-2026-001")).thenReturn(List.of(makeResponse()));

        ResponseEntity<ApiResponse<List<DocumentResponse>>> response = controller.getDocuments("AR-2026-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    @Test
    void getDocumentsByArReturnsEmptyList() {
        when(service.getDocuments("AR-999")).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<DocumentResponse>>> response = controller.getDocuments("AR-999");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().data().isEmpty());
    }

    // --- GET listAll (200 OK) ---

    @Test
    void listAllDocumentsReturns200() {
        when(service.listAllDocuments()).thenReturn(List.of(makeResponse()));

        var response = controller.listAllDocuments();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().data().size());
    }

    // --- GET download (200 OK with file) ---

    @Test
    void downloadReturns200WithResource() throws Exception {
        Path tempFile = Files.createTempFile("download-test", ".pdf");
        Files.write(tempFile, "test-content".getBytes());

        Resource resource = new FileSystemResource(tempFile);

        // Controller calls getDocumentMeta first, then downloadDocument
        when(service.getDocumentMeta(1L)).thenReturn(makeResponse());
        when(service.downloadDocument(1L)).thenReturn(resource);

        ResponseEntity<Resource> response = controller.downloadDocument(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().exists());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());

        Files.deleteIfExists(tempFile);
    }

    @Test
    void downloadNotFoundPropagates404() {
        when(service.getDocumentMeta(99L))
                .thenThrow(new EntityNotFoundException("DocumentRegistry", 99L));

        assertThrows(EntityNotFoundException.class, () -> controller.downloadDocument(99L));
    }

    // --- Business rule violation propagation ---

    @Test
    void uploadRoaBusinessRuleViolationPropagates() throws Exception {
        when(service.uploadRoa(anyLong(), any(MultipartFile.class)))
                .thenThrow(new BusinessRuleViolationException("File type not supported"));

        assertThrows(BusinessRuleViolationException.class, () ->
                controller.uploadRoa(mockPdfFile(), 1L));
    }

    @Test
    void uploadCoaBusinessRuleViolationPropagates() throws Exception {
        when(service.uploadCoa(anyString(), any(MultipartFile.class)))
                .thenThrow(new BusinessRuleViolationException("File exceeds size limit"));

        assertThrows(BusinessRuleViolationException.class, () ->
                controller.uploadCoa(mockPdfFile(), "AR-2026-001"));
    }

    // --- ApiResponse envelope consistency ---

    @Test
    void allJsonResponsesUseApiResponseEnvelope() {
        when(service.listAllDocuments()).thenReturn(List.of(makeResponse()));

        var response = controller.listAllDocuments();
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertNotNull(response.getBody().timestamp());
        assertEquals("Success", response.getBody().message());
    }
}
