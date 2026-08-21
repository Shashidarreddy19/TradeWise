package com.trade.exception;

import java.util.Map;

/**
 * Thrown when service-layer validation detects field-specific errors
 * (e.g. duplicate email, invalid GST format for the selected country).
 *
 * The GlobalExceptionHandler serializes this as:
 * { "success": false, "message": "Validation failed", "data": { "field": "error message" } }
 */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = fieldErrors;
    }

    public FieldValidationException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
