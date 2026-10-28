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
    ERR_INVALID_METRIC("The specified metric ID does not exist."),
    ERR_RESTRICTED_DELETE("Cannot delete; record is referenced by other data."),
    ERR_INVALID_EMPLOYEE_ID("The specified employee ID does not exist."),
    ERR_QUALIFICATION_DUPLICATE("This employee is already qualified for this parameter."),
    ERR_INVENTORY_DUPLICATE("This product-parameter combination already exists."),
    ERR_COUNT_MUST_BE_POSITIVE("Count needed must be greater than 0."),
    ERR_NO_VALID_ROWS("No valid rows found in the uploaded file."),
    ERR_FILE_TOO_LARGE("Uploaded file exceeds the maximum row limit."),
    ERR_INACTIVE_RECORD("The provided ID belongs to an inactive record. You cannot update or deactivate a record that is already inactive."),
    ERR_METRIC_BOUNDS_EMPTY("At least one bound (Lower Bound or Upper Bound) must be provided."),
    ERR_BOUNDS_INVALID("Lower bound must be less than or equal to upper bound."),
    ERR_CANNOT_CHANGE_TO_TEXT("Cannot change Result Type to TEXT. Active metric specifications exist for this configuration."),
    ERR_CONFIG_NOT_FOUND("The specified configuration ID does not exist."),
    ERR_CONFIG_NOT_METRICS("The specified configuration does not have result type METRICS; metric specifications are only permitted for METRICS."),
    ERR_METRIC_SPEC_DUPLICATE("This configuration already has an active metric specification for this metric."),
    ERR_TEST_METHOD_REQUIRED("Test Method ID is required."),
    ERR_INVALID_RESULT_TYPE("Result type must be TEXT or METRICS."),
    ERR_PRODUCT_NOT_FOUND_OR_INACTIVE("Product ID is invalid or inactive."),
    ERR_PRODUCT_SERVICE_UNAVAILABLE("Product validation service is temporarily unavailable. Please try again later."),
    ERR_QUALIFICATION_DUPLICATE_IN_FILE("Duplicate employee and parameter combination in uploaded file."),
    ERR_INVENTORY_DUPLICATE_IN_FILE("Duplicate product and parameter combination in uploaded file."),
    ERR_METRIC_SPEC_DUPLICATE_IN_FILE("Duplicate configuration and metric combination in uploaded file.");

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
