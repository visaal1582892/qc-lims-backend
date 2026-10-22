package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.response.DocumentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Document Management service interface (SRS Module 11).
 */
public interface DocumentManagementService {

    /**
     * Upload ROA (Record of Analysis) PDF linked to execution_id.
     */
    DocumentResponse uploadRoa(Long executionId, MultipartFile file);

    /**
     * Upload COA (Certificate of Analysis) PDF linked to ar_number.
     */
    DocumentResponse uploadCoa(String arNumber, MultipartFile file);

    /**
     * List all documents for an AR.
     */
    List<DocumentResponse> getDocuments(String arNumber);

    /**
     * List all active documents (global view).
     */
    List<DocumentResponse> listAllDocuments();

    /**
     * Download document by file ID.
     */
    Resource downloadDocument(Long fileId);

    /**
     * Get the original file name and content type for download.
     */
    DocumentResponse getDocumentMeta(Long fileId);
}
