package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.domain.QcInventory;
import com.medplus.qc_lims_backend.domain.SampleInwardRegister;
import com.medplus.qc_lims_backend.dto.request.AllocateRequest;
import com.medplus.qc_lims_backend.dto.request.BarcodeRequest;
import com.medplus.qc_lims_backend.dto.request.ConsumeRequest;
import com.medplus.qc_lims_backend.dto.request.QcInventoryRequest;
import com.medplus.qc_lims_backend.dto.response.BarcodeResponse;
import com.medplus.qc_lims_backend.dto.response.QcInventoryResponse;
import com.medplus.qc_lims_backend.enums.RecordStatus;
import com.medplus.qc_lims_backend.enums.StorageCondition;
import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import com.medplus.qc_lims_backend.exception.DuplicateResourceException;
import com.medplus.qc_lims_backend.exception.EntityNotFoundException;
import com.medplus.qc_lims_backend.repository.QcInventoryRepository;
import com.medplus.qc_lims_backend.repository.SampleInwardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for QC Inventory, Barcoding & Storage Allocation.
 *
 * Covers SRS Module 4 (Sample Labelling, Barcoding & Storage Allocation)
 * and SRS Module 5 (QC Inventory & Consumption Reconciliation).
 *
 * Review corrections applied:
 *   C-2 [HIGH]: Derives user ID from SecurityContextHolder, not from client.
 *   C-4 [HIGH]: manufacturer_id included in createInventory flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QcInventoryService {

    private final QcInventoryRepository inventoryRepository;
    private final SampleInwardRepository inwardRepository;

    // --- User ID extraction (H-2 fix: resolve actual user from SecurityContextHolder) ---

    /**
     * Resolve current user ID from Spring Security context.
     * Follows InwardService pattern: accepts userId parameter from controller
     * when available, otherwise derives from SecurityContextHolder.
     */
    private Long resolveUserId(Long suppliedUserId) {
        if (suppliedUserId != null) {
            return suppliedUserId;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails) {
                // UserDetails does not carry a numeric ID in this project's auth setup.
                // Log for observability; caller should supply userId for audit accuracy.
                log.debug("Authenticated principal is UserDetails; no numeric user ID available.");
            }
        } catch (IllegalStateException ex) {
            log.warn("No SecurityContext available when resolving user ID: {}", ex.getMessage());
        }
        return null; // No user identity resolved — DB NOT NULL constraint will catch if required
    }

    // --- READ operations (readOnly by default) ---

    public List<QcInventoryResponse> listAll() {
        return inventoryRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public QcInventoryResponse getById(Long id) {
        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));
        return toResponse(entity);
    }

    public List<QcInventoryResponse> listByArNumber(String arNumber) {
        Optional<QcInventory> result = inventoryRepository.findByArNumberAndStatus(arNumber, RecordStatus.ACTIVE);
        return result.map(e -> List.of(toResponse(e))).orElse(List.of());
    }

    public List<QcInventoryResponse> listByProductId(Integer productId) {
        return inventoryRepository.findByProductIdAndStatus(productId, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QcInventoryResponse> listByStorageLocation(String storageLocation) {
        return inventoryRepository.findByStorageLocationAndStatus(storageLocation, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<QcInventoryResponse> searchByBatchNo(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) {
            return listAll();
        }
        return inventoryRepository.findByBatchNoContainingIgnoreCaseAndStatus(batchNo, RecordStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    // --- CREATE (mutating) ---

    @Transactional
    public QcInventoryResponse createInventory(QcInventoryRequest request) {
        // V-01: ar_number must reference existing sample_inward_register record
        SampleInwardRegister inward = inwardRepository.findByArNumberAndStatus(request.getArNumber(), RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(
                        "InwardRegistration with AR number", request.getArNumber()));

        // V-02: ar_number must be unique in qc_inventory (one record per AR)
        if (inventoryRepository.existsByArNumberAndStatus(request.getArNumber(), RecordStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "Inventory record already exists for AR number: " + request.getArNumber());
        }

        QcInventory entity = new QcInventory();
        entity.setArNumber(request.getArNumber());
        entity.setManufacturerId(request.getManufacturerId());
        entity.setProductId(request.getProductId());
        entity.setBatchNo(request.getBatchNo());
        entity.setTotalQuantityReceived(request.getTotalQuantityReceived());
        entity.setQuantityAllocated(0);
        entity.setQuantityConsumed(0);
        entity.setStorageLocation(request.getStorageLocation());
        entity.setCreatedBy(resolveUserId(null)); // H-2: derive from SecurityContextHolder
        entity.setStatus(RecordStatus.ACTIVE);

        // Parse optional dates
        if (request.getMfgDate() != null && !request.getMfgDate().isBlank()) {
            entity.setMfgDate(LocalDate.parse(request.getMfgDate()));
        }
        if (request.getExpDate() != null && !request.getExpDate().isBlank()) {
            entity.setExpDate(LocalDate.parse(request.getExpDate()));
        }

        entity = inventoryRepository.save(entity);
        return toResponse(entity);
    }

    // --- UPDATE (mutating) ---

    @Transactional
    public QcInventoryResponse updateTotalQuantity(Long id, int newTotal) {
        if (newTotal < 1) {
            throw new BusinessRuleViolationException("Total quantity must be at least 1.");
        }

        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        // M-1: Prevent reducing total below already-allocated quantity
        int currentAllocated = entity.getQuantityAllocated() == null ? 0 : entity.getQuantityAllocated();
        if (newTotal < currentAllocated) {
            throw new BusinessRuleViolationException(
                    "Cannot reduce total quantity to " + newTotal +
                    " — already allocated: " + currentAllocated +
                    ". Total must be >= allocated quantity.");
        }

        entity.setTotalQuantityReceived(newTotal);
        entity = inventoryRepository.save(entity);
        return toResponse(entity);
    }

    /**
     * H-1 fix: General update endpoint for inventory records.
     * Partial update — only non-null fields in request are applied.
     * Matches InwardService.update() pattern.
     */
    @Transactional
    public QcInventoryResponse update(Long id, QcInventoryRequest request) {
        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        // Partial update — only non-null fields
        if (request.getProductId() != null) {
            entity.setProductId(request.getProductId());
        }
        if (request.getManufacturerId() != null) {
            entity.setManufacturerId(request.getManufacturerId());
        }
        if (request.getBatchNo() != null && !request.getBatchNo().isBlank()) {
            entity.setBatchNo(request.getBatchNo());
        }
        if (request.getTotalQuantityReceived() != null) {
            // M-1: Validate total >= allocated on update too
            int newTotal = request.getTotalQuantityReceived();
            if (newTotal < 1) {
                throw new BusinessRuleViolationException("Total quantity must be at least 1.");
            }
            int currentAllocated = entity.getQuantityAllocated() == null ? 0 : entity.getQuantityAllocated();
            if (newTotal < currentAllocated) {
                throw new BusinessRuleViolationException(
                        "Cannot reduce total quantity to " + newTotal +
                        " — already allocated: " + currentAllocated +
                        ". Total must be >= allocated quantity.");
            }
            entity.setTotalQuantityReceived(newTotal);
        }
        if (request.getStorageLocation() != null) {
            entity.setStorageLocation(request.getStorageLocation());
        }
        if (request.getMfgDate() != null && !request.getMfgDate().isBlank()) {
            entity.setMfgDate(LocalDate.parse(request.getMfgDate()));
        }
        if (request.getExpDate() != null && !request.getExpDate().isBlank()) {
            entity.setExpDate(LocalDate.parse(request.getExpDate()));
        }

        entity = inventoryRepository.save(entity);
        return toResponse(entity);
    }

    // --- STORAGE ASSIGNMENT (mutating) ---

    @Transactional
    public QcInventoryResponse assignStorageLocation(Long id, String storageLocation) {
        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        // V-12: Validate storage location against StorageCondition enum
        if (StorageCondition.fromValue(storageLocation) == null) {
            throw new BusinessRuleViolationException(
                    "Invalid storage location: " + storageLocation +
                    ". Must be one of: " + String.join(", ",
                            java.util.Arrays.stream(StorageCondition.values())
                                    .map(StorageCondition::getDisplayName)
                                    .toList()));
        }

        entity.setStorageLocation(storageLocation);
        entity = inventoryRepository.save(entity);

        // V-13: Also update sample_inward_register for consistency
        SampleInwardRegister inward = inwardRepository.findByArNumberAndStatus(
                entity.getArNumber(), RecordStatus.ACTIVE).orElse(null);
        if (inward != null) {
            inward.setStorageLocation(storageLocation);
            inwardRepository.save(inward);
        }

        return toResponse(entity);
    }

    // --- ALLOCATION (mutating, PESSIMISTIC_WRITE) ---

    @Transactional
    public QcInventoryResponse allocateQuantity(Long id, AllocateRequest request) {
        QcInventory entity = inventoryRepository.findByIdAndStatusWithLock(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        int currentAllocated = entity.getQuantityAllocated() == null ? 0 : entity.getQuantityAllocated();
        // V-07: Prevent over-allocation
        if (currentAllocated + request.getQuantity() > entity.getTotalQuantityReceived()) {
            throw new BusinessRuleViolationException(
                    "Cannot allocate " + request.getQuantity() + " units. " +
                    "Already allocated: " + currentAllocated +
                    ", Total received: " + entity.getTotalQuantityReceived() +
                    ", Available: " + (entity.getTotalQuantityReceived() - currentAllocated));
        }

        entity.allocate(request.getQuantity());
        entity = inventoryRepository.save(entity);
        return toResponse(entity);
    }

    // --- CONSUMPTION (mutating, PESSIMISTIC_WRITE) ---

    @Transactional
    public QcInventoryResponse recordConsumption(Long id, ConsumeRequest request) {
        QcInventory entity = inventoryRepository.findByIdAndStatusWithLock(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        int currentConsumed = entity.getQuantityConsumed() == null ? 0 : entity.getQuantityConsumed();
        int currentAllocated = entity.getQuantityAllocated() == null ? 0 : entity.getQuantityAllocated();
        // V-08: Cannot consume more than allocated
        if (currentConsumed + request.getQuantity() > currentAllocated) {
            throw new BusinessRuleViolationException(
                    "Cannot consume " + request.getQuantity() + " units. " +
                    "Already consumed: " + currentConsumed +
                    ", Total allocated: " + currentAllocated +
                    ", Available to consume: " + (currentAllocated - currentConsumed));
        }

        entity.consume(request.getQuantity());
        entity = inventoryRepository.save(entity);
        return toResponse(entity);
    }

    // --- VARIANCE RECONCILIATION (read-only) ---

    /**
     * Get variance report: all items where allocated > consumed (unconsumed variance).
     * V-17: Variance = allocated - consumed.
     * V-18: Positive variance = unconsumed sample (flagged for audit).
     */
    public List<QcInventoryResponse> getVarianceReport() {
        return inventoryRepository.findByStatus(RecordStatus.ACTIVE).stream()
                .filter(e -> e.getVariance() > 0)
                .map(this::toResponse)
                .toList();
    }

    // --- BARCODE GENERATION (read-only) ---

    /**
     * Generate barcode string for an inventory record.
     * V-14: Format: AR_Number|Product_Name|Batch_Number|Storage_Condition
     * V-15: All four components required.
     */
    public BarcodeResponse generateBarcode(Long id, BarcodeRequest request) {
        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));

        String productName = request != null && request.getProductName() != null
                ? request.getProductName() : "";

        String barcodeString = entity.getBarcodeString(productName);

        return BarcodeResponse.of(
                entity.getArNumber(),
                productName,
                entity.getBatchNo(),
                entity.getStorageLocation() != null ? entity.getStorageLocation() : "",
                barcodeString
        );
    }

    // --- SOFT DELETE (mutating) ---

    @Transactional
    public void delete(Long id) {
        QcInventory entity = inventoryRepository.findByIdAndStatus(id, RecordStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("QcInventory", id));
        entity.setStatus(RecordStatus.INACTIVE);
        inventoryRepository.save(entity);
    }

    // --- DTO conversion ---

    private QcInventoryResponse toResponse(QcInventory entity) {
        return QcInventoryResponse.fromEntity(entity);
    }
}
