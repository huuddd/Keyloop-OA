package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.customer.VehicleId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VehicleIdConverter implements AttributeConverter<VehicleId, Long> {

    @Override
    public Long convertToDatabaseColumn(VehicleId id) {
        return id == null ? null : id.value();
    }

    @Override
    public VehicleId convertToEntityAttribute(Long value) {
        return value == null ? null : VehicleId.of(value);
    }
}
