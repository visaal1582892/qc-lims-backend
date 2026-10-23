package com.medplus.qc_lims_backend.enums;

/**
 * Master table status — 'A' (Active) / 'I' (Inactive) — per goal.md schema.
 * Converted via MasterStatusJpaConverter.
 */
public enum MasterStatus {
    ACTIVE('A'),
    INACTIVE('I');

    private final char dbValue;

    MasterStatus(char dbValue) {
        this.dbValue = dbValue;
    }

    public char getDbValue() {
        return dbValue;
    }
}
