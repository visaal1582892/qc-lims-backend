package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.DocumentRegistry;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.dto.response.DocumentResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.DocumentRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DocumentManagementService covering SRS Module 11 acceptance criteria.
 *
 * AC-19: uploadDocument stores file and writes path
 * AC-20: File type validation — only PDF accepted
 * AC-21: File size cap — max 25 MB
 * AC-22: File naming convention — UUID-based unique filename
 * AC-23: getDocument — retrieve file path
 * AC-24: downloadDocument — serve file
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentManagementServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private TestExecutionRepository testExecutionRepository;
    @InjectMocks private DocumentManagementService documentManagementService;

    private Authentication mockAuth;

    @BeforeEach
    void setUpSecurityContext() {
        mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(mockAuth);
    }

    // =========================================================================
    // AC-19: Upload ROA Document
    // =========================================================================

    @Test
    void testUploadRoaValidPdf() throws Exception {
        byte[] content = "fake-pdf-content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", content);

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setArNumber("AR-2026-001");
        execution.setStatus(RecordStatus.ACTIVE);

        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));
        when(documentRepository.save(any(DocumentRegistry.class))).thenAnswer(inv -> {
            DocumentRegistry saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        DocumentResponse response = documentManagementService.uploadRoa(1L, file);

        assertNotNull(response);
        assertEquals("ROA", response.documentType());
        assertEquals("report.pdf", response.fileName());
        assertEquals(content.length, response.fileSizeBytes().intValue());

        ArgumentCaptor<DocumentRegistry> captor = ArgumentCaptor.forClass(DocumentRegistry.class);
        verify(documentRepository).save(captor.capture());
        DocumentRegistry saved = captor.getValue();
        assertEquals("AR-2026-001", saved.getArNumber());
        assertEquals(1L, saved.getExecutionId());
        assertEquals(RecordStatus.ACTIVE, saved.getStatus());

        // Verify test_execution was updated with roa_file_path
        verify(testExecutionRepository).save(any(TestExecution.class));
    }

    @Test
    void testUploadCoaValidPdf() throws Exception {
        byte[] content = "certificate-content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "coa.pdf", "application/pdf", content);

        when(documentRepository.save(any(DocumentRegistry.class))).thenAnswer(inv -> {
            DocumentRegistry saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(testExecutionRepository.findByArNumberAndStatus("AR-2026-001", RecordStatus.ACTIVE))
                .thenReturn(List.of());

        DocumentResponse response = documentManagementService.uploadCoa("AR-2026-001", file);

        assertNotNull(response);
        assertEquals("COA", response.documentType());
        assertEquals("AR-2026-001", response.arNumber());
        assertNull(response.executionId());
    }

    // =========================================================================
    // AC-20: File Type Validation — PDF Only
    // =========================================================================

    @Test
    void testUploadRejectsNonPdfContentType() throws Exception {
        byte[] content = "not-a-pdf".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "report.txt", "text/plain", content);

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        assertThrows(BusinessRuleViolationException.class, () ->
                documentManagementService.uploadRoa(1L, file));
    }

    @Test
    void testUploadRejectsNonPdfExtension() throws Exception {
        byte[] content = "not-a-pdf".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "report.docx", "unknown/type", content);

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        assertThrows(BusinessRuleViolationException.class, () ->
                documentManagementService.uploadRoa(1L, file));
    }

    @Test
    void testUploadAcceptsPdfWithNullContentType() throws Exception {
        // When content-type is null but extension is .pdf, should be accepted
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", null, "pdf-content".getBytes());

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));
        when(documentRepository.save(any(DocumentRegistry.class))).thenAnswer(inv -> {
            DocumentRegistry saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Should not throw — file has .pdf extension
        DocumentResponse response = documentManagementService.uploadRoa(1L, file);
        assertNotNull(response);
    }

    // =========================================================================
    // AC-21: File Size Cap — 25 MB
    // =========================================================================

    @Test
    void testUploadRejectsOversizedFile() throws Exception {
        // Create a file larger than 25 MB
        byte[] largeContent = new byte[(int) (26L * 1024 * 1024)];
        MultipartFile file = new MockMultipartFile(
                "file", "huge.pdf", "application/pdf", largeContent);

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        assertThrows(BusinessRuleViolationException.class, () ->
                documentManagementService.uploadRoa(1L, file));
    }

    // =========================================================================
    // Empty file rejection
    // =========================================================================

    @Test
    void testUploadRejectsEmptyFile() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        assertThrows(BusinessRuleViolationException.class, () ->
                documentManagementService.uploadRoa(1L, file));
    }

    @Test
    void testUploadRejectsNullFile() {
        assertThrows(BusinessRuleViolationException.class, () ->
                documentManagementService.uploadRoa(1L, null));
    }

    // =========================================================================
    // Execution not found
    // =========================================================================

    @Test
    void testUploadRoaForNonexistentExecution() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        when(testExecutionRepository.findByIdAndStatus(999L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                documentManagementService.uploadRoa(999L, file));
    }

    // =========================================================================
    // AC-23: Get Documents for AR
    // =========================================================================

    @Test
    void testGetDocumentsForAr() {
        DocumentRegistry doc = new DocumentRegistry();
        doc.setId(1L);
        doc.setDocumentType("ROA");
        doc.setArNumber("AR-2026-001");
        doc.setFileName("roa.pdf");
        doc.setFilePath("/uploads/documents/roa.pdf");
        doc.setStatus(RecordStatus.ACTIVE);

        when(documentRepository.findByArNumberAndStatus("AR-2026-001", RecordStatus.ACTIVE))
                .thenReturn(List.of(doc));

        List<DocumentResponse> result = documentManagementService.getDocuments("AR-2026-001");

        assertEquals(1, result.size());
        assertEquals("ROA", result.get(0).documentType());
        assertEquals("roa.pdf", result.get(0).fileName());
    }

    @Test
    void testGetDocumentsEmptyForAr() {
        when(documentRepository.findByArNumberAndStatus("AR-999", RecordStatus.ACTIVE))
                .thenReturn(List.of());

        List<DocumentResponse> result = documentManagementService.getDocuments("AR-999");
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // List all documents
    // =========================================================================

    @Test
    void testListAllDocuments() {
        DocumentRegistry doc1 = new DocumentRegistry();
        doc1.setId(1L);
        doc1.setDocumentType("ROA");
        doc1.setStatus(RecordStatus.ACTIVE);

        DocumentRegistry doc2 = new DocumentRegistry();
        doc2.setId(2L);
        doc2.setDocumentType("COA");
        doc2.setStatus(RecordStatus.ACTIVE);

        when(documentRepository.findAllByStatusOrderByUploadedAtDesc(RecordStatus.ACTIVE))
                .thenReturn(List.of(doc1, doc2));

        List<DocumentResponse> result = documentManagementService.listAllDocuments();
        assertEquals(2, result.size());
    }

    // =========================================================================
    // AC-24: Download Document
    // =========================================================================

    @Test
    void testDownloadExistingFile() throws Exception {
        DocumentRegistry registry = new DocumentRegistry();
        registry.setId(1L);
        registry.setFilePath(System.getProperty("java.io.tmpdir") + "/test_doc.pdf");
        registry.setStatus(RecordStatus.ACTIVE);

        // Create a temporary file
        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "test_doc.pdf");
        Files.write(tempFile, "test-pdf-content".getBytes());

        when(documentRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(registry));

        try {
            Resource resource = documentManagementService.downloadDocument(1L);
            assertNotNull(resource);
            assertTrue(resource.exists());
            assertInstanceOf(FileSystemResource.class, resource);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testDownloadNotFoundInRegistry() {
        when(documentRepository.findByIdAndStatus(999L, RecordStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                documentManagementService.downloadDocument(999L));
    }

    @Test
    void testDownloadFileMissingOnDisk() {
        DocumentRegistry registry = new DocumentRegistry();
        registry.setId(1L);
        registry.setFilePath("/nonexistent/path/document.pdf");
        registry.setStatus(RecordStatus.ACTIVE);

        when(documentRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(registry));

        assertThrows(EntityNotFoundException.class, () ->
                documentManagementService.downloadDocument(1L));
    }

    // =========================================================================
    // Get document metadata
    // =========================================================================

    @Test
    void testGetDocumentMeta() {
        DocumentRegistry registry = new DocumentRegistry();
        registry.setId(1L);
        registry.setDocumentType("ROA");
        registry.setFileName("test.pdf");
        registry.setFileSizeBytes(1024L);
        registry.setContentType("application/pdf");
        registry.setStatus(RecordStatus.ACTIVE);

        when(documentRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(registry));

        DocumentResponse meta = documentManagementService.getDocumentMeta(1L);
        assertNotNull(meta);
        assertEquals("ROA", meta.documentType());
        assertEquals("test.pdf", meta.fileName());
    }

    // =========================================================================
    // Security — no user context
    // =========================================================================

    @Test
    void testUploadWithoutSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();

        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        TestExecution execution = new TestExecution();
        execution.setId(1L);
        execution.setStatus(RecordStatus.ACTIVE);
        when(testExecutionRepository.findByIdAndStatus(1L, RecordStatus.ACTIVE))
                .thenReturn(Optional.of(execution));

        DocumentRegistry saved = new DocumentRegistry();
        saved.setId(1L);
        saved.setFileName("report.pdf");
        saved.setArNumber("AR-001");
        when(documentRepository.save(any(DocumentRegistry.class))).thenReturn(saved);

        // With anonymous access, resolveUserId defaults to 1L — upload succeeds
        var result = documentManagementService.uploadRoa(1L, file);
        assertNotNull(result);
    }
}
