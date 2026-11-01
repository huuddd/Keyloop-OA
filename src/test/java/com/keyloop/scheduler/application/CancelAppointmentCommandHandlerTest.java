package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.command.CancelAppointmentCommand;
import com.keyloop.scheduler.application.appointment.command.CancelAppointmentCommandHandler;
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
class CancelAppointmentCommandHandlerTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private EventPublisher eventPublisher;

    private Clock clock;
    private CancelAppointmentCommandHandler handler;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        handler = new CancelAppointmentCommandHandler(appointmentRepo, eventPublisher, clock);
    }

    private Appointment buildConfirmedAppointment(UUID id) {
        return Appointment.reconstruct(
            AppointmentId.of(id),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(2),
            AppointmentStatus.CONFIRMED, FIXED_NOW, FIXED_NOW);
    }

    @Test
    void successfulCancel() {
        var id = UUID.randomUUID();
        var appointment = buildConfirmedAppointment(id);
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void> result = handler.handle(new CancelAppointmentCommand(id));

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepo).save(appointment);
        verify(eventPublisher).publishEventsFor(appointment);
    }

    @Test
    void notFound_returnsFailure() {
        var id = UUID.randomUUID();
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.empty());

        Result<Void> result = handler.handle(new CancelAppointmentCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.NotFound");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void invalidTransition_returnsFailure() {
        var id = UUID.randomUUID();
        // Build a COMPLETED appointment -- cannot cancel
        var appointment = Appointment.reconstruct(
            AppointmentId.of(id),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(2),
            AppointmentStatus.COMPLETED, FIXED_NOW, FIXED_NOW);
        when(appointmentRepo.findById(AppointmentId.of(id)))
            .thenReturn(Optional.of(appointment));

        Result<Void> result = handler.handle(new CancelAppointmentCommand(id));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.InvalidTransition");
        verify(appointmentRepo, never()).save(any());
    }
}
