package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DealershipIdConverter implements AttributeConverter<DealershipId, Long> {

    @Override
    public Long convertToDatabaseColumn(DealershipId id) {
        return id == null ? null : id.value();
    }

    @Override
    public DealershipId convertToEntityAttribute(Long value) {
        return value == null ? null : DealershipId.of(value);
    }
}
