package com.keyloop.scheduler.application.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final List<String> errors;

    private ValidationResult(List<String> errors) {
        this.errors = Collections.unmodifiableList(errors);
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult withError(String error) {
        return new ValidationResult(List.of(error));
    }

    public static ValidationResult withErrors(List<String> errors) {
        return new ValidationResult(new ArrayList<>(errors));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public ValidationResult merge(ValidationResult other) {
        var merged = new ArrayList<>(this.errors);
        merged.addAll(other.errors);
        return new ValidationResult(merged);
    }
}
