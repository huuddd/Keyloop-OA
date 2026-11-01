package com.keyloop.scheduler.application.validation;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ValidatingService {

    private final Map<Class<?>, List<CommandValidator<?>>> validatorMap;

    public ValidatingService(List<CommandValidator<?>> validators) {
        this.validatorMap = validators.stream()
            .collect(Collectors.groupingBy(CommandValidator::getCommandType));
    }

    @SuppressWarnings("unchecked")
    public <T> ValidationResult validate(T command) {
        var validators = validatorMap.getOrDefault(command.getClass(), List.of());
        return validators.stream()
            .map(v -> ((CommandValidator<T>) v).validate(command))
            .reduce(ValidationResult::merge)
            .orElse(ValidationResult.valid());
    }
}
