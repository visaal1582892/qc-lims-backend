package com.medplus.qc_lims_backend.enums;

/**
 * Storage condition values per SRS Module 4 (Sample Labelling, Barcoding & Storage Allocation).
 * Physical storage bin location in QC lab: Ambient, Refrigerator (2-8°C), Freezer (-23 to -13°C).
 */
public enum StorageCondition {
    AMBIENT("Ambient", "Room temperature (20-25°C)"),
    REFRIGERATOR("Refrigerator", "Refrigerated (2-8°C)"),
    FREEZER("Freezer", "Frozen (-23 to -13°C)"),
    OTHERS("Others", "Other storage condition");

    private final String displayName;
    private final String temperatureRange;

    StorageCondition(String displayName, String temperatureRange) {
        this.displayName = displayName;
        this.temperatureRange = temperatureRange;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTemperatureRange() {
        return temperatureRange;
    }

    /**
     * Resolve a string value to a StorageCondition enum.
     * @return matching enum or null if not found
     */
    public static StorageCondition fromValue(String value) {
        if (value == null) return null;
        for (StorageCondition sc : values()) {
            if (sc.name().equalsIgnoreCase(value) || sc.displayName.equalsIgnoreCase(value)) {
                return sc;
            }
        }
        return null;
    }
}
