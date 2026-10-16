package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.ReceiptChecklistRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.ReceiptChecklistResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.ReceiptChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Receipt Checklist (physical verification).
 */
@RestController
@RequestMapping("/api/receipt-checklist")
@RequiredArgsConstructor
public class ReceiptChecklistController {

    private final ReceiptChecklistService checklistService;

    @PreAuthorize("hasAuthority('" + Rights.ROLE_RECEIPT_CHECKLIST_VIEW + "')")
    @GetMapping("/trf/{trfId}")
    public ResponseEntity<ApiResponse<ReceiptChecklistResponse>> getByTrfId(@PathVariable Integer trfId) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.getByTrfId(trfId)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_RECEIPT_CHECKLIST_CREATE + "')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReceiptChecklistResponse>> create(
            @Valid @RequestBody ReceiptChecklistRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.create(request, null)));
    }

    @PreAuthorize("hasAuthority('" + Rights.ROLE_RECEIPT_CHECKLIST_UPDATE + "')")
    @PutMapping("/{checklistId}")
    public ResponseEntity<ApiResponse<ReceiptChecklistResponse>> update(
            @PathVariable Long checklistId,
            @Valid @RequestBody ReceiptChecklistRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(checklistService.update(checklistId, request, null)));
    }
}
