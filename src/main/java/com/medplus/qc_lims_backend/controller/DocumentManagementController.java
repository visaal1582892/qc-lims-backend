package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.DocumentResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.DocumentManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Document Management controller (SRS Module 11).
 *
 * Endpoints:
 * - POST /api/v1/documents/roa            — Upload ROA (DM-01)
 * - POST /api/v1/documents/coa            — Upload COA (DM-02)
 * - GET  /api/v1/documents/{arNumber}     — List documents for AR (DM-03)
 * - GET  /api/v1/documents/download/{id}  — Download document (DM-04)
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentManagementController {

    private final DocumentManagementService documentManagementService;

    /**
     * DM-01: Upload ROA (Record of Analysis) PDF linked to execution_id.
     */
    @PostMapping(value = "/roa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_DOCUMENT_UPLOAD')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadRoa(
            @RequestParam("file") MultipartFile file,
            @RequestParam("executionId") Long executionId) {
        DocumentResponse response = documentManagementService.uploadRoa(executionId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * DM-02: Upload COA (Certificate of Analysis) PDF linked to ar_number.
     */
    @PostMapping(value = "/coa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_DOCUMENT_UPLOAD')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadCoa(
            @RequestParam("file") MultipartFile file,
            @RequestParam("arNumber") String arNumber) {
        DocumentResponse response = documentManagementService.uploadCoa(arNumber, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * DM-03: List all documents for an AR number.
     */
    @GetMapping("/{arNumber}")
    @PreAuthorize("hasAuthority('ROLE_DOCUMENT_VIEW')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @PathVariable String arNumber) {
        List<DocumentResponse> documents = documentManagementService.getDocuments(arNumber);
        return ResponseEntity.ok(ApiResponse.ok(documents));
    }

    /**
     * DM-06: List all documents (global view).
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_DOCUMENT_VIEW')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listAllDocuments() {
        List<DocumentResponse> documents = documentManagementService.listAllDocuments();
        return ResponseEntity.ok(ApiResponse.ok(documents));
    }

    /**
     * DM-04: Download a document by file ID.
     * Returns the file as an inline attachment.
     */
    @GetMapping("/download/{fileId}")
    @PreAuthorize("hasAuthority('ROLE_DOCUMENT_VIEW')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long fileId) {
        DocumentResponse meta = documentManagementService.getDocumentMeta(fileId);
        Resource resource = documentManagementService.downloadDocument(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.fileName() + "\"")
                .body(resource);
    }
}
