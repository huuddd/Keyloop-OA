package com.keyloop.scheduler.application.validation;

public interface CommandValidator<T> {
    ValidationResult validate(T command);
    Class<T> getCommandType();
}
