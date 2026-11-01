package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceTypeIdConverter implements AttributeConverter<ServiceTypeId, Long> {

    @Override
    public Long convertToDatabaseColumn(ServiceTypeId id) {
        return id == null ? null : id.value();
    }

    @Override
    public ServiceTypeId convertToEntityAttribute(Long value) {
        return value == null ? null : ServiceTypeId.of(value);
    }
}
