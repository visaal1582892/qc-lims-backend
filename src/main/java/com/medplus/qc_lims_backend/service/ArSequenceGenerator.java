package com.medplus.qc_lims_backend.service;

import com.medplus.qc_lims_backend.exception.BusinessRuleViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Dedicated service for atomic AR number generation.
 *
 * Uses INSERT ... ON CONFLICT (calendar_year) DO UPDATE SET last_sequence = last_sequence + 1
 * RETURNING last_sequence — a single atomic statement with no race condition (RC-4).
 *
 * Calendar year stored as 2-digit string ('26') consistent with V1 seed.
 */
@Component
@RequiredArgsConstructor
public class ArSequenceGenerator {

    private final EntityManager entityManager;

    @Transactional
    public String generateArNumber() {
        String calendarYear = String.valueOf(LocalDate.now().getYear()).substring(2); // '26'

        try {
            // Atomic: INSERT ... ON CONFLICT DO UPDATE ... RETURNING
            // This is a SINGLE statement — no race condition (RC-4)
            // PostgreSQL guarantees row-level lock during ON CONFLICT resolution
            Query query = entityManager.createNativeQuery(
                "INSERT INTO ar_sequence_tracker (calendar_year, last_sequence) " +
                "VALUES (:year, 1) " +
                "ON CONFLICT (calendar_year) DO UPDATE SET last_sequence = last_sequence + 1 " +
                "RETURNING last_sequence"
            );
            query.setParameter("year", calendarYear);

            Number result = (Number) Objects.requireNonNull(query.getSingleResult());
            int sequence = result.intValue();

            // Validate overflow at 9999
            if (sequence > 9999) {
                throw new BusinessRuleViolationException(
                    "AR sequence overflow for year " + calendarYear +
                    ". Maximum 9999 sequences reached. Contact system administrator."
                );
            }

            return String.format("MHSL%s%04d", calendarYear, sequence);
        } catch (JpaSystemException ex) {
            // Handle PessimisticLockingFailureException or lock timeout (RC-5)
            // JpaSystemException has a single constructor (RuntimeException) in Spring ORM 7.x,
            // so we cannot wrap it with a custom message. Re-throw directly.
            if (ex.getMessage() != null && ex.getMessage().contains("PessimisticLockingFailure")) {
                throw new RuntimeException(
                    "Another operation is currently in progress. Please retry.", ex);
            }
            throw ex;
        }
    }

    /**
     * Generate an OOS incident number: OOS-{year}{seq} (e.g. OOS-260001).
     * Uses the same atomic INSERT ... ON CONFLICT pattern as AR number generation.
     */
    @Transactional
    public String generateOosNumber() {
        String calendarYear = String.valueOf(LocalDate.now().getYear()).substring(2); // '26'

        try {
            Query query = entityManager.createNativeQuery(
                "INSERT INTO oos_sequence_tracker (calendar_year, last_sequence) " +
                "VALUES (:year, 1) " +
                "ON CONFLICT (calendar_year) DO UPDATE SET last_sequence = last_sequence + 1 " +
                "RETURNING last_sequence"
            );
            query.setParameter("year", calendarYear);

            Number result = (Number) Objects.requireNonNull(query.getSingleResult());
            int sequence = result.intValue();

            if (sequence > 9999) {
                throw new BusinessRuleViolationException(
                    "OOS sequence overflow for year " + calendarYear +
                    ". Maximum 9999 sequences reached. Contact system administrator."
                );
            }

            return String.format("OOS-%s%04d", calendarYear, sequence);
        } catch (JpaSystemException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("PessimisticLockingFailure")) {
                throw new RuntimeException(
                    "Another operation is currently in progress. Please retry.", ex);
            }
            throw ex;
        }
    }
}
