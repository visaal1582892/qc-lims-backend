package com.medplus.qc_lims_backend.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for MasterStatus (A/I) ↔ DB VARCHAR(1).
 * Auto-applied so entities don't need @Convert annotations.
 */
@Converter(autoApply = true)
public class MasterStatusJpaConverter implements AttributeConverter<MasterStatus, String> {

    @Override
    public String convertToDatabaseColumn(MasterStatus attribute) {
        return attribute == null ? null : String.valueOf(attribute.getDbValue());
    }

    @Override
    public MasterStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        for (MasterStatus s : MasterStatus.values()) {
            if (String.valueOf(s.getDbValue()).equals(dbData)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown MasterStatus value: " + dbData);
    }
}
