package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.query.GetAppointmentQuery;
import com.keyloop.scheduler.application.appointment.query.GetAppointmentQueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.domain.appointment.*;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAppointmentQueryHandlerTest {

    @Mock private AppointmentRepository appointmentRepo;
    @InjectMocks private GetAppointmentQueryHandler handler;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Test
    void found_returnsDto() {
        var id = UUID.randomUUID();
        var appointment = Appointment.reconstruct(
            AppointmentId.of(id),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(2),
            AppointmentStatus.CONFIRMED, FIXED_NOW, FIXED_NOW);
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.of(appointment));

        AppointmentDto dto = handler.handle(new GetAppointmentQuery(id));

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void notFound_throwsEntityNotFoundException() {
        var id = UUID.randomUUID();
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new GetAppointmentQuery(id)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());
    }
}
