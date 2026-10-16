package com.medplus.qc_lims_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Disposition for OOS incidents (oos_incident_register.final_disposition).
 *
 * Lifecycle: Pending → Retest_Authorized → Retest_Passed | Batch_Rejected.
 * 'Retest_Passed' tracks successful retest resolution so the original OOS
 * record retains a proper audit trail (SRS Module 8).
 */
public enum Disposition {
    Pending("Pending"),
    Retest_Authorized("Retest_Authorized"),
    Retest_Passed("Retest_Passed"),
    Batch_Rejected("Batch_Rejected");

    private final String value;

    Disposition(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Disposition fromValue(String value) {
        for (Disposition d : values()) {
            if (d.value.equalsIgnoreCase(value)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown Disposition: " + value);
    }
}
