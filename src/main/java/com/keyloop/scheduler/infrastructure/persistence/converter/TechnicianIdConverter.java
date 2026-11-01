package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.dealership.TechnicianId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TechnicianIdConverter implements AttributeConverter<TechnicianId, Long> {

    @Override
    public Long convertToDatabaseColumn(TechnicianId id) {
        return id == null ? null : id.value();
    }

    @Override
    public TechnicianId convertToEntityAttribute(Long value) {
        return value == null ? null : TechnicianId.of(value);
    }
}
