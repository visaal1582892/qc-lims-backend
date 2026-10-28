package com.medplus.qc_lims_backend.util;

/**
 * SQL utilities for safe query parameter handling.
 */
public final class SqlUtils {

    private SqlUtils() {}

    /**
     * Escapes special characters ('\', '%', '_') in the input and wraps in '%' for LIKE search.
     * Prevents unintended wildcard expansion by treating input as literal text.
     *
     * @param input Raw search keyword
     * @return Sanitized pattern suitable for case-insensitive LIKE search, or null if input is empty
     */
    public static String escapeLike(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String sanitized = input.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + sanitized.toLowerCase() + "%";
    }
}
