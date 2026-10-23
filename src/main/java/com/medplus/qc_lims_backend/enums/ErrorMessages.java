package com.medplus.qc_lims_backend.enums;

/**
 * Centralized error messages — goal.md §2.3, rules.md §3.3.
 */
public enum ErrorMessages {
    ERR_MISSING_ID("ID is mandatory for Update (U) and Delete (D) actions."),
    ERR_ID_NOT_ALLOWED("ID must be blank for Create (A) actions."),
    ERR_INVALID_ACTION("Action must be 'A', 'U', or 'D'."),
    ERR_PARAM_NOT_FOUND("The specified parameter ID does not exist."),
    ERR_DUPLICATE_ENTRY("This combination already exists in the system."),
    ERR_ENTITY_NOT_FOUND("The specified record does not exist."),
    ERR_INVALID_STATUS("Status must be 'A' (Active) or 'I' (Inactive)."),
    ERR_MISSING_REQUIRED_FIELD("A required field is missing."),
    ERR_INVALID_METRIC_UNIT("The specified metric unit ID does not exist."),
    ERR_RESTRICTED_DELETE("Cannot delete; record is referenced by other data."),
    ERR_INVALID_EMPLOYEE_ID("The specified employee ID does not exist."),
    ERR_QUALIFICATION_DUPLICATE("This employee is already qualified for this parameter."),
    ERR_INVENTORY_DUPLICATE("This product-parameter combination already exists."),
    ERR_COUNT_MUST_BE_POSITIVE("Count needed must be greater than 0."),
    ERR_NO_VALID_ROWS("No valid rows found in the uploaded file."),
    ERR_FILE_TOO_LARGE("Uploaded file exceeds the maximum row limit."),
    ERR_EXTERNAL_TOS_FAILED("Failed to send TOS notification to external system."),
    ERR_EXTERNAL_TOS_UPDATE_FAILED("Failed to send TOS update to external system."),
    ERR_EXTERNAL_SYSTEM_UNAVAILABLE("External system is currently unavailable.");

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
