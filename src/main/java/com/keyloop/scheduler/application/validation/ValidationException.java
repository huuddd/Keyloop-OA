package com.keyloop.scheduler.application.validation;

public class ValidationException extends RuntimeException {

    private final ValidationResult validationResult;

    public ValidationException(ValidationResult validationResult) {
        super("Validation failed: " + String.join(", ", validationResult.getErrors()));
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
