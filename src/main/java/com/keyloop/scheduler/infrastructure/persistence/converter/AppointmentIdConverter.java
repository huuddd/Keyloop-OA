package com.keyloop.scheduler.infrastructure.persistence.converter;

import com.keyloop.scheduler.domain.appointment.AppointmentId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter(autoApply = true)
public class AppointmentIdConverter implements AttributeConverter<AppointmentId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(AppointmentId id) {
        return id == null ? null : id.value();
    }

    @Override
    public AppointmentId convertToEntityAttribute(UUID value) {
        return value == null ? null : AppointmentId.of(value);
    }
}
