package com.medplus.qc_lims_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * QC Inventory entity — maps to qc_inventory table (SRS Module 5, Module 13 lines 358-371).
 *
 * Extends BaseEntity for id, createdAt, updatedAt, createdBy, updatedBy, status.
 * Overrides: BaseEntity.id -> inventory_id, BaseEntity.createdBy -> received_by_user_id.
 *
 * current_balance is a PostgreSQL GENERATED STORED column — mapped read-only.
 * manufacturer_id included per SRS Module 5.1 (review correction C-4).
 */
@Entity
@Table(name = "qc_inventory")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class QcInventory extends BaseEntity {

    @Column(name = "inventory_id")
    @EqualsAndHashCode.Include
    private Long id;

    /** Override inherited createdBy column name to received_by_user_id */
    @Column(name = "received_by_user_id")
    private Long createdBy;

    @Column(name = "ar_number", length = 50)
    private String arNumber;

    /** SRS Module 5.1: manufacturer_id references Company DB manufacturer_master */
    @Column(name = "manufacturer_id", nullable = false)
    private Integer manufacturerId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "batch_no", nullable = false, length = 100)
    private String batchNo;

    @Column(name = "total_quantity_received", nullable = false)
    private Integer totalQuantityReceived;

    @Column(name = "quantity_allocated")
    private Integer quantityAllocated = 0;

    @Column(name = "quantity_consumed")
    private Integer quantityConsumed = 0;

    /** PostgreSQL GENERATED STORED column — JPA must not INSERT/UPDATE this */
    @Column(name = "current_balance", insertable = false, updatable = false)
    private Integer currentBalance;

    @Column(name = "storage_location", length = 100)
    private String storageLocation;

    @Column(name = "mfg_date")
    private LocalDate mfgDate;

    @Column(name = "exp_date")
    private LocalDate expDate;

    // --- Business logic methods ---

    /**
     * Allocate sample quantity to an analyst.
     * Validates: allocated + delta <= total (no over-allocation).
     */
    public void allocate(int quantity) {
        this.quantityAllocated = (this.quantityAllocated == null ? 0 : this.quantityAllocated) + quantity;
    }

    /**
     * Record consumed quantity.
     * Validates: consumed + delta <= allocated (cannot exceed allocated).
     */
    public void consume(int quantity) {
        this.quantityConsumed = (this.quantityConsumed == null ? 0 : this.quantityConsumed) + quantity;
    }

    /**
     * Compute variance = allocated - consumed.
     * Positive variance means unconsumed sample (flagged for audit).
     */
    public int getVariance() {
        int allocated = this.quantityAllocated == null ? 0 : this.quantityAllocated;
        int consumed = this.quantityConsumed == null ? 0 : this.quantityConsumed;
        return allocated - consumed;
    }

    /**
     * Generate barcode string: AR_Number|Product_Name|Batch_Number|Storage_Condition
     * per SRS Module 4 line 123.
     */
    public String getBarcodeString(String productName) {
        String ar = arNumber != null ? arNumber : "";
        String batch = batchNo != null ? batchNo : "";
        String storage = storageLocation != null ? storageLocation : "";
        String product = productName != null ? productName : "";
        return String.format("%s|%s|%s|%s", ar, product, batch, storage);
    }
}
