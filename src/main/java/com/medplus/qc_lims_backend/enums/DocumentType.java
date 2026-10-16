package com.medplus.qc_lims_backend.enums;

/**
 * Document types tracked in the document registry (SRS Module 11).
 */
public enum DocumentType {

    ROA("ROA"),
    COA("COA");

    private final String dbValue;

    DocumentType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static DocumentType fromDbValue(String dbValue) {
        for (DocumentType d : values()) {
            if (d.dbValue.equals(dbValue)) return d;
        }
        throw new IllegalArgumentException("Unknown DocumentType: " + dbValue);
    }
}
