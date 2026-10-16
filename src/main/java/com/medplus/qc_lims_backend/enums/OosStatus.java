package com.medplus.qc_lims_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OOS investigation status (oos_incident_register.oos_status).
 *
 * Tracks the workflow state of an OOS investigation from creation through
 * final resolution.
 */
public enum OosStatus {
    PENDING("Pending"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    ESCALATED("Escalated");

    private final String value;

    OosStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OosStatus fromValue(String value) {
        for (OosStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown OosStatus: " + value);
    }
}
