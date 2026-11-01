package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceBayIdConverter implements AttributeConverter<ServiceBayId, Long> {

    @Override
    public Long convertToDatabaseColumn(ServiceBayId id) {
        return id == null ? null : id.value();
    }

    @Override
    public ServiceBayId convertToEntityAttribute(Long value) {
        return value == null ? null : ServiceBayId.of(value);
    }
}
