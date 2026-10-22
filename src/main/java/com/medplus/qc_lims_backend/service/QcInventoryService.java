package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.dto.request.AllocateRequest;
import com.medplus.qc_lims_backend.dto.request.BarcodeRequest;
import com.medplus.qc_lims_backend.dto.request.ConsumeRequest;
import com.medplus.qc_lims_backend.dto.request.QcInventoryRequest;
import com.medplus.qc_lims_backend.dto.response.BarcodeResponse;
import com.medplus.qc_lims_backend.dto.response.QcInventoryResponse;

import java.util.List;

/**
 * Service layer interface for QC Inventory, Barcoding & Storage Allocation.
 */
public interface QcInventoryService {

    List<QcInventoryResponse> listAll();

    QcInventoryResponse getById(Long id);

    List<QcInventoryResponse> listByArNumber(String arNumber);

    List<QcInventoryResponse> listByProductId(Integer productId);

    List<QcInventoryResponse> listByStorageLocation(String storageLocation);

    List<QcInventoryResponse> searchByBatchNo(String batchNo);

    QcInventoryResponse createInventory(QcInventoryRequest request);

    QcInventoryResponse updateTotalQuantity(Long id, int newTotal);

    QcInventoryResponse update(Long id, QcInventoryRequest request);

    QcInventoryResponse assignStorageLocation(Long id, String storageLocation);

    QcInventoryResponse allocateQuantity(Long id, AllocateRequest request);

    QcInventoryResponse recordConsumption(Long id, ConsumeRequest request);

    List<QcInventoryResponse> getVarianceReport();

    BarcodeResponse generateBarcode(Long id, BarcodeRequest request);

    void delete(Long id);
}
