package com.medplus.qc_lims_backend.controller;

import com.medplus.qc_lims_backend.dto.request.AllocateRequest;
import com.medplus.qc_lims_backend.dto.request.BarcodeRequest;
import com.medplus.qc_lims_backend.dto.request.ConsumeRequest;
import com.medplus.qc_lims_backend.dto.request.QcInventoryRequest;
import com.medplus.qc_lims_backend.dto.response.ApiResponse;
import com.medplus.qc_lims_backend.dto.response.BarcodeResponse;
import com.medplus.qc_lims_backend.dto.response.QcInventoryResponse;
import com.medplus.qc_lims_backend.security.Rights;
import com.medplus.qc_lims_backend.service.QcInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for QC Inventory, Barcoding & Storage Allocation (Modules 4 & 5).
 *
 * Endpoints:
 *   Inventory CRUD: GET/POST /api/inventory, GET/PUT/DELETE /api/inventory/{id}
 *   Storage: PUT /api/inventory/{id}/storage, GET /api/inventory/storage/{location}
 *   Allocation: POST /api/inventory/{id}/allocate
 *   Consumption: POST /api/inventory/{id}/consume
 *   Variance: GET /api/inventory/variance
 *   Barcode: GET /api/inventory/{id}/barcode, POST /api/inventory/{id}/barcode
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class QcInventoryController {

    private final QcInventoryService inventoryService;

    // --- Inventory CRUD ---

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_READ + \"')\"")
    @GetMapping
    public ResponseEntity<ApiResponse<List<QcInventoryResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listAll()));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_READ + \"')\"")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QcInventoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getById(id)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_READ + \"')\"")
    @GetMapping("/search/batch")
    public ResponseEntity<ApiResponse<List<QcInventoryResponse>>> searchByBatch(@RequestParam String batchNo) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.searchByBatchNo(batchNo)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_CREATE + \"')\"")
    @PostMapping
    public ResponseEntity<ApiResponse<QcInventoryResponse>> create(@Valid @RequestBody QcInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(inventoryService.createInventory(request)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_DELETE + \"')\"")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // --- Storage allocation ---

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_STORAGE_ASSIGN + \"')\"")
    @PutMapping("/{id}/storage")
    public ResponseEntity<ApiResponse<QcInventoryResponse>> assignStorage(
            @PathVariable Long id,
            @RequestParam String storageLocation) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.assignStorageLocation(id, storageLocation)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_STORAGE_VIEW + \"')\"")
    @GetMapping("/storage/{location}")
    public ResponseEntity<ApiResponse<List<QcInventoryResponse>>> listByStorage(
            @PathVariable String location) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listByStorageLocation(location)));
    }

    // --- Allocation / Consumption ---

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_ALLOCATE + \"')\"")
    @PostMapping("/{id}/allocate")
    public ResponseEntity<ApiResponse<QcInventoryResponse>> allocate(
            @PathVariable Long id,
            @Valid @RequestBody AllocateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.allocateQuantity(id, request)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_CONSUME + \"')\"")
    @PostMapping("/{id}/consume")
    public ResponseEntity<ApiResponse<QcInventoryResponse>> consume(
            @PathVariable Long id,
            @Valid @RequestBody ConsumeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.recordConsumption(id, request)));
    }

    // --- Variance report ---

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_INVENTORY_VARIANCE + \"')\"")
    @GetMapping("/variance")
    public ResponseEntity<ApiResponse<List<QcInventoryResponse>>> getVarianceReport() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getVarianceReport()));
    }

    // --- Barcode ---

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_BARCODE_GENERATE + \"')\"")
    @GetMapping("/{id}/barcode")
    public ResponseEntity<ApiResponse<BarcodeResponse>> getBarcode(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.generateBarcode(id, null)));
    }

    @PreAuthorize("hasAuthority('\"' + Rights.ROLE_BARCODE_GENERATE + \"')\"")
    @PostMapping("/{id}/barcode")
    public ResponseEntity<ApiResponse<BarcodeResponse>> generateBarcode(
            @PathVariable Long id,
            @Valid @RequestBody BarcodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.generateBarcode(id, request)));
    }
}
