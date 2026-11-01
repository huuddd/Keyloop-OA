package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.command.MarkNoShowCommand;
import com.keyloop.scheduler.application.appointment.command.MarkNoShowCommandHandler;
import com.keyloop.scheduler.application.port.EventPublisher;
import com.keyloop.scheduler.domain.appointment.*;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkNoShowCommandHandlerTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private EventPublisher eventPublisher;

    private Clock clock;
    private MarkNoShowCommandHandler handler;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        handler = new MarkNoShowCommandHandler(appointmentRepo, eventPublisher, clock);
    }

    @Test
    void successfulNoShow() {
        var id = UUID.randomUUID();
        var appointment = Appointment.reconstruct(
            AppointmentId.of(id),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(2),
            AppointmentStatus.CONFIRMED, FIXED_NOW, FIXED_NOW);
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void> result = handler.handle(new MarkNoShowCommand(id));

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        verify(appointmentRepo).save(appointment);
        verify(eventPublisher).publishEventsFor(appointment);
    }
}
