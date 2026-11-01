package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.appointment.*;
import com.keyloop.scheduler.domain.appointment.event.*;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.Technician;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import com.keyloop.scheduler.domain.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    // Fixed clock: 2025-06-15T10:00:00
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    // Future start time (1 hour from "now")
    private static final LocalDateTime FUTURE_START = NOW.plusHours(1);

    // Past start time
    private static final LocalDateTime PAST_START = NOW.minusHours(1);

    // --- Test helpers ---

    private static Technician qualifiedTechnician() {
        return new Technician(
                TechnicianId.of(1L),
                DealershipId.of(1L),
                "Jane Mechanic",
                Set.of(Specialization.GENERAL, Specialization.ENGINE));
    }

    private static Technician unqualifiedTechnician() {
        return new Technician(
                TechnicianId.of(2L),
                DealershipId.of(1L),
                "Bob Painter",
                Set.of(Specialization.BRAKES));
    }

    private static ServiceType oilChangeService() {
        return new ServiceType(
                ServiceTypeId.of(1L),
                "Oil Change",
                30,
                Specialization.GENERAL);
    }

    private static Result<Appointment> createValidAppointment() {
        return Appointment.create(
                CustomerId.of(1L),
                VehicleId.of(1L),
                ServiceBayId.of(1L),
                TechnicianId.of(1L),
                oilChangeService(),
                qualifiedTechnician(),
                FUTURE_START,
                FIXED_CLOCK);
    }

    // --- Tests ---

    @Test
    @DisplayName("create() succeeds with valid inputs, status is CONFIRMED, domain event raised")
    void createSucceedsWithValidInputs() {
        Result<Appointment> result = createValidAppointment();

        assertThat(result.isSuccess()).isTrue();
        Appointment apt = result.getValue();
        assertThat(apt.getId()).isNotNull();
        assertThat(apt.getCustomerId()).isEqualTo(CustomerId.of(1L));
        assertThat(apt.getVehicleId()).isEqualTo(VehicleId.of(1L));
        assertThat(apt.getServiceBayId()).isEqualTo(ServiceBayId.of(1L));
        assertThat(apt.getTechnicianId()).isEqualTo(TechnicianId.of(1L));
        assertThat(apt.getServiceTypeId()).isEqualTo(ServiceTypeId.of(1L));
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(apt.getStartTime()).isEqualTo(FUTURE_START);
        assertThat(apt.getEndTime()).isEqualTo(FUTURE_START.plusMinutes(30));
        assertThat(apt.getCreatedAt()).isEqualTo(NOW);
        assertThat(apt.getUpdatedAt()).isEqualTo(NOW);

        // Domain event raised
        assertThat(apt.getDomainEvents()).hasSize(1);
        assertThat(apt.getDomainEvents().get(0)).isInstanceOf(AppointmentCreatedEvent.class);
        AppointmentCreatedEvent event = (AppointmentCreatedEvent) apt.getDomainEvents().get(0);
        assertThat(event.appointmentId()).isEqualTo(apt.getId().value());
        assertThat(event.customerId()).isEqualTo(1L);
        assertThat(event.serviceBayId()).isEqualTo(1L);
        assertThat(event.technicianId()).isEqualTo(1L);
        assertThat(event.startTime()).isEqualTo(FUTURE_START);
        assertThat(event.endTime()).isEqualTo(FUTURE_START.plusMinutes(30));
    }

    @Test
    @DisplayName("create() fails with unqualified technician")
    void createFailsWithUnqualifiedTechnician() {
        Result<Appointment> result = Appointment.create(
                CustomerId.of(1L),
                VehicleId.of(1L),
                ServiceBayId.of(1L),
                TechnicianId.of(2L),
                oilChangeService(),
                unqualifiedTechnician(),
                FUTURE_START,
                FIXED_CLOCK);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.TechnicianNotQualified");
    }

    @Test
    @DisplayName("create() fails with startTime in past")
    void createFailsWithStartTimeInPast() {
        Result<Appointment> result = Appointment.create(
                CustomerId.of(1L),
                VehicleId.of(1L),
                ServiceBayId.of(1L),
                TechnicianId.of(1L),
                oilChangeService(),
                qualifiedTechnician(),
                PAST_START,
                FIXED_CLOCK);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.StartTimeInPast");
    }

    @Test
    @DisplayName("Full lifecycle: CONFIRMED -> checkIn -> IN_PROGRESS -> complete -> COMPLETED")
    void fullLifecycle() {
        Appointment apt = createValidAppointment().getValue();
        apt.clearDomainEvents();

        // Check in
        Result<Void> checkInResult = apt.checkIn(FIXED_CLOCK);
        assertThat(checkInResult.isSuccess()).isTrue();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(apt.getDomainEvents()).hasSize(1);
        assertThat(apt.getDomainEvents().get(0)).isInstanceOf(AppointmentCheckedInEvent.class);

        apt.clearDomainEvents();

        // Complete
        Result<Void> completeResult = apt.complete(FIXED_CLOCK);
        assertThat(completeResult.isSuccess()).isTrue();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(apt.getDomainEvents()).hasSize(1);
        assertThat(apt.getDomainEvents().get(0)).isInstanceOf(AppointmentCompletedEvent.class);
    }

    @Test
    @DisplayName("Cancel from CONFIRMED -> CANCELLED")
    void cancelFromConfirmed() {
        Appointment apt = createValidAppointment().getValue();
        apt.clearDomainEvents();

        Result<Void> result = apt.cancel(FIXED_CLOCK);
        assertThat(result.isSuccess()).isTrue();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(apt.getDomainEvents()).hasSize(1);
        assertThat(apt.getDomainEvents().get(0)).isInstanceOf(AppointmentCancelledEvent.class);

        AppointmentCancelledEvent event = (AppointmentCancelledEvent) apt.getDomainEvents().get(0);
        assertThat(event.appointmentId()).isEqualTo(apt.getId().value());
        assertThat(event.customerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Cancel from IN_PROGRESS -> CANCELLED")
    void cancelFromInProgress() {
        Appointment apt = createValidAppointment().getValue();
        apt.checkIn(FIXED_CLOCK);
        apt.clearDomainEvents();

        Result<Void> result = apt.cancel(FIXED_CLOCK);
        assertThat(result.isSuccess()).isTrue();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("No-show from CONFIRMED -> NO_SHOW")
    void noShowFromConfirmed() {
        Appointment apt = createValidAppointment().getValue();
        apt.clearDomainEvents();

        Result<Void> result = apt.markNoShow(FIXED_CLOCK);
        assertThat(result.isSuccess()).isTrue();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(apt.getDomainEvents()).hasSize(1);
        assertThat(apt.getDomainEvents().get(0)).isInstanceOf(AppointmentNoShowEvent.class);

        AppointmentNoShowEvent event = (AppointmentNoShowEvent) apt.getDomainEvents().get(0);
        assertThat(event.appointmentId()).isEqualTo(apt.getId().value());
        assertThat(event.customerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Invalid: complete from CONFIRMED (must be IN_PROGRESS first)")
    void completeFromConfirmedFails() {
        Appointment apt = createValidAppointment().getValue();

        Result<Void> result = apt.complete(FIXED_CLOCK);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.InvalidTransition");
    }

    @Test
    @DisplayName("Invalid: checkIn from COMPLETED (terminal state)")
    void checkInFromCompletedFails() {
        Appointment apt = createValidAppointment().getValue();
        apt.checkIn(FIXED_CLOCK);
        apt.complete(FIXED_CLOCK);

        Result<Void> result = apt.checkIn(FIXED_CLOCK);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.InvalidTransition");
    }

    @Test
    @DisplayName("Invalid: cancel from COMPLETED (terminal state)")
    void cancelFromCompletedFails() {
        Appointment apt = createValidAppointment().getValue();
        apt.checkIn(FIXED_CLOCK);
        apt.complete(FIXED_CLOCK);

        Result<Void> result = apt.cancel(FIXED_CLOCK);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.InvalidTransition");
    }

    @Test
    @DisplayName("reconstruct() creates appointment without validation or events")
    void reconstructCreatesWithoutValidationOrEvents() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime end = start.plusMinutes(60);
        LocalDateTime created = LocalDateTime.of(2025, 1, 1, 8, 0);
        LocalDateTime updated = LocalDateTime.of(2025, 1, 1, 9, 30);

        Appointment apt = Appointment.reconstruct(
                AppointmentId.of(java.util.UUID.randomUUID()),
                CustomerId.of(10L),
                VehicleId.of(20L),
                ServiceBayId.of(3L),
                TechnicianId.of(5L),
                ServiceTypeId.of(2L),
                start, end,
                AppointmentStatus.IN_PROGRESS,
                created, updated);

        assertThat(apt.getId()).isNotNull();
        assertThat(apt.getCustomerId()).isEqualTo(CustomerId.of(10L));
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(apt.getStartTime()).isEqualTo(start);
        assertThat(apt.getEndTime()).isEqualTo(end);
        assertThat(apt.getCreatedAt()).isEqualTo(created);
        assertThat(apt.getUpdatedAt()).isEqualTo(updated);
        // No domain events should be raised
        assertThat(apt.getDomainEvents()).isEmpty();
    }
}
