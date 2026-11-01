package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.customer.CustomerId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CustomerIdConverter implements AttributeConverter<CustomerId, Long> {

    @Override
    public Long convertToDatabaseColumn(CustomerId id) {
        return id == null ? null : id.value();
    }

    @Override
    public CustomerId convertToEntityAttribute(Long value) {
        return value == null ? null : CustomerId.of(value);
    }
}
