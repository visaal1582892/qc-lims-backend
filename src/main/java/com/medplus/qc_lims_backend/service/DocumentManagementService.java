package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.DocumentRegistry;
import com.medplus.qc_lims_backend.domain.TestExecution;
import com.medplus.qc_lims_backend.dto.response.DocumentResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.DocumentRepository;
import com.medplus.qc_lims_backend.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Document Management service (SRS Module 11).
 *
 * Responsibilities:
 * - Upload ROA (Record of Analysis) PDF linked to execution_id
 * - Upload COA (Certificate of Analysis) PDF linked to ar_number
 * - List all documents for an AR
 * - Download documents by file ID
 * - File validation: PDF type, size limit (25 MB)
 *
 * Files are stored on the server filesystem under a configured directory.
 * Paths are recorded in document_registry and also in test_execution
 * (roa_file_path / coa_file_path) for backwards compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentManagementService {

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024; // 25 MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final String UPLOAD_DIR = "uploads/documents";

    private final DocumentRepository documentRepository;
    private final TestExecutionRepository testExecutionRepository;

    // ========================================================================
    // DM-01: Upload ROA (Record of Analysis)
    // ========================================================================

    @Transactional
    public DocumentResponse uploadRoa(Long executionId, MultipartFile file) {
        // Validate file
        validateFile(file);

        // Verify execution exists
        TestExecution execution = testExecutionRepository
                .findByIdAndStatus(executionId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("TestExecution", executionId));

        // Store file
        Path filePath = storeFile(file);

        // Create registry record
        DocumentRegistry registry = new DocumentRegistry();
        registry.setDocumentType("ROA");
        registry.setArNumber(execution.getArNumber());
        registry.setExecutionId(executionId);
        registry.setFileName(file.getOriginalFilename());
        registry.setFilePath(filePath.toString());
        registry.setFileSizeBytes(file.getSize());
        registry.setContentType(file.getContentType() != null ? file.getContentType() : ALLOWED_CONTENT_TYPE);
        registry.setUploadedBy(resolveUserId());
        registry.setUploadedAt(Instant.now());
        registry.setStatus(RecordStatus.ACTIVE);

        DocumentRegistry saved = documentRepository.save(registry);

        // Update test_execution with roa_file_path
        execution.setRoaFilePath(filePath.toString());
        testExecutionRepository.save(execution);

        log.info("ROA uploaded for execution {}: {}", executionId, saved.getFileName());
        return DocumentResponse.fromEntity(saved);
    }

    // ========================================================================
    // DM-02: Upload COA (Certificate of Analysis)
    // ========================================================================

    @Transactional
    public DocumentResponse uploadCoa(String arNumber, MultipartFile file) {
        // Validate file
        validateFile(file);

        // Store file
        Path filePath = storeFile(file);

        // Create registry record
        DocumentRegistry registry = new DocumentRegistry();
        registry.setDocumentType("COA");
        registry.setArNumber(arNumber);
        registry.setExecutionId(null);
        registry.setFileName(file.getOriginalFilename());
        registry.setFilePath(filePath.toString());
        registry.setFileSizeBytes(file.getSize());
        registry.setContentType(file.getContentType() != null ? file.getContentType() : ALLOWED_CONTENT_TYPE);
        registry.setUploadedBy(resolveUserId());
        registry.setUploadedAt(Instant.now());
        registry.setStatus(RecordStatus.ACTIVE);

        DocumentRegistry saved = documentRepository.save(registry);

        // Update test_execution with coa_file_path for the first matching execution
        testExecutionRepository
                .findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE)
                .stream()
                .findFirst()
                .ifPresent(execution -> {
                    execution.setCoaFilePath(filePath.toString());
                    testExecutionRepository.save(execution);
                });

        log.info("COA uploaded for AR {}: {}", arNumber, saved.getFileName());
        return DocumentResponse.fromEntity(saved);
    }

    // ========================================================================
    // DM-03: Get Documents for an AR
    // ========================================================================

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String arNumber) {
        List<DocumentRegistry> documents = documentRepository
                .findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE);
        return documents.stream()
                .map(DocumentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * List all active documents (global view).
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> listAllDocuments() {
        List<DocumentRegistry> documents = documentRepository
                .findAllByStatusOrderByUploadedAtDesc(RecordStatus.ACTIVE);
        return documents.stream()
                .map(DocumentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // DM-04: Download Document
    // ========================================================================

    @Transactional(readOnly = true)
    public Resource downloadDocument(Long fileId) {
        DocumentRegistry registry = documentRepository
                .findByIdAndStatus(fileId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("DocumentRegistry", fileId));

        Path filePath = Paths.get(registry.getFilePath());
        if (!Files.exists(filePath)) {
            throw new EntityNotFoundException("Document file", registry.getFilePath());
        }

        return new FileSystemResource(filePath);
    }

    /**
     * Get the original file name and content type for download.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentMeta(Long fileId) {
        DocumentRegistry registry = documentRepository
                .findByIdAndStatus(fileId, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("DocumentRegistry", fileId));
        return DocumentResponse.fromEntity(registry);
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Validate uploaded file: must be PDF and under 25 MB.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("File cannot be empty");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPE.equals(contentType)) {
            // Also check file extension as fallback
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                throw new BusinessRuleViolationException(
                        "Only PDF files are allowed. Received: " + (contentType != null ? contentType : "unknown type"));
            }
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleViolationException(
                    "File size exceeds 25 MB limit. Received: " + (file.getSize() / (1024 * 1024)) + " MB");
        }
    }

    /**
     * Store uploaded file to the server filesystem.
     * Creates directory structure if needed.
     */
    private Path storeFile(MultipartFile file) {
        try {
            // Create upload directory structure: uploads/documents/{year}/{month}
            java.time.YearMonth now = java.time.YearMonth.now();
            Path dir = Paths.get(UPLOAD_DIR, String.valueOf(now.getYear()), String.valueOf(now.getMonthValue()));
            Files.createDirectories(dir);

            // Generate unique file name
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".pdf";
            String uniqueName = UUID.randomUUID() + extension;

            Path targetPath = dir.resolve(uniqueName);
            Files.write(targetPath, file.getBytes());

            log.info("File stored at: {}", targetPath);
            return targetPath;
        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new BusinessRuleViolationException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Resolve the current user ID from SecurityContextHolder.
     */
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        log.warn("Could not resolve user ID from SecurityContext — defaulting to 1");
        return 1L;
    }
}
