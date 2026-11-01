package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.command.CreateAppointmentCommand;
import com.keyloop.scheduler.application.appointment.command.CreateAppointmentCommandHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.port.EventPublisher;
import com.keyloop.scheduler.application.validation.ValidatingService;
import com.keyloop.scheduler.application.validation.ValidationResult;
import com.keyloop.scheduler.domain.appointment.Appointment;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.dealership.*;
import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import com.keyloop.scheduler.domain.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentCommandHandlerTest {

    @Mock private AppointmentRepository appointmentRepo;
    @Mock private ServiceBayRepository serviceBayRepo;
    @Mock private TechnicianRepository technicianRepo;
    @Mock private ServiceTypeRepository serviceTypeRepo;
    @Mock private EventPublisher eventPublisher;
    @Mock private ValidatingService validatingService;

    private Clock clock;
    private CreateAppointmentCommandHandler handler;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
            FIXED_NOW.toInstant(ZoneOffset.UTC),
            ZoneId.of("UTC"));
        handler = new CreateAppointmentCommandHandler(
            appointmentRepo, serviceBayRepo, technicianRepo,
            serviceTypeRepo, eventPublisher, validatingService, clock);
    }

    @Test
    void happyPath_returnsSuccessWithDto() {
        // Given
        var command = new CreateAppointmentCommand(1L, 1L, 1L, 1L,
            FIXED_NOW.plusHours(1));

        when(validatingService.validate(command)).thenReturn(ValidationResult.valid());

        var serviceType = new ServiceType(
            ServiceTypeId.of(1L), "Oil Change", 30, Specialization.GENERAL);
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L)))
            .thenReturn(Optional.of(serviceType));

        var bay = new ServiceBay(ServiceBayId.of(1L), DealershipId.of(1L), "Bay 1", true);
        when(serviceBayRepo.findActiveByDealershipIdWithLock(DealershipId.of(1L)))
            .thenReturn(List.of(bay));

        when(appointmentRepo.findOverlappingForBayWithLock(
            eq(ServiceBayId.of(1L)), any(), any()))
            .thenReturn(List.of());

        var tech = new Technician(TechnicianId.of(1L), DealershipId.of(1L),
            "John", Set.of(Specialization.GENERAL));
        when(technicianRepo.findByDealershipIdAndSpecialization(
            DealershipId.of(1L), Specialization.GENERAL))
            .thenReturn(List.of(tech));

        when(appointmentRepo.findOverlappingForTechnicianWithLock(
            eq(TechnicianId.of(1L)), any(), any()))
            .thenReturn(List.of());

        when(appointmentRepo.save(any(Appointment.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        Result<AppointmentDto> result = handler.handle(command);

        // Then
        assertThat(result.isSuccess()).isTrue();
        var dto = result.getValue();
        assertThat(dto.customerId()).isEqualTo(1L);
        assertThat(dto.vehicleId()).isEqualTo(1L);
        assertThat(dto.serviceBayId()).isEqualTo(1L);
        assertThat(dto.technicianId()).isEqualTo(1L);
        assertThat(dto.status()).isEqualTo("CONFIRMED");

        verify(appointmentRepo).save(any(Appointment.class));
        verify(eventPublisher).publishEventsFor(any(Appointment.class));
    }

    @Test
    void noBayAvailable_returnsFailure() {
        // Given
        var command = new CreateAppointmentCommand(1L, 1L, 1L, 1L,
            FIXED_NOW.plusHours(1));

        when(validatingService.validate(command)).thenReturn(ValidationResult.valid());

        var serviceType = new ServiceType(
            ServiceTypeId.of(1L), "Oil Change", 30, Specialization.GENERAL);
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L)))
            .thenReturn(Optional.of(serviceType));

        // No bays available
        when(serviceBayRepo.findActiveByDealershipIdWithLock(DealershipId.of(1L)))
            .thenReturn(List.of());

        // When
        Result<AppointmentDto> result = handler.handle(command);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.NoBayAvailable");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void noTechAvailable_returnsFailure() {
        // Given
        var command = new CreateAppointmentCommand(1L, 1L, 1L, 1L,
            FIXED_NOW.plusHours(1));

        when(validatingService.validate(command)).thenReturn(ValidationResult.valid());

        var serviceType = new ServiceType(
            ServiceTypeId.of(1L), "Oil Change", 30, Specialization.GENERAL);
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L)))
            .thenReturn(Optional.of(serviceType));

        var bay = new ServiceBay(ServiceBayId.of(1L), DealershipId.of(1L), "Bay 1", true);
        when(serviceBayRepo.findActiveByDealershipIdWithLock(DealershipId.of(1L)))
            .thenReturn(List.of(bay));

        when(appointmentRepo.findOverlappingForBayWithLock(
            eq(ServiceBayId.of(1L)), any(), any()))
            .thenReturn(List.of());

        // No technicians available
        when(technicianRepo.findByDealershipIdAndSpecialization(
            DealershipId.of(1L), Specialization.GENERAL))
            .thenReturn(List.of());

        // When
        Result<AppointmentDto> result = handler.handle(command);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.NoTechnicianAvailable");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void domainValidationFailure_pastTime_returnsFailure() {
        // Given: startTime is in the past
        var command = new CreateAppointmentCommand(1L, 1L, 1L, 1L,
            FIXED_NOW.minusHours(1));

        when(validatingService.validate(command)).thenReturn(ValidationResult.valid());

        var serviceType = new ServiceType(
            ServiceTypeId.of(1L), "Oil Change", 30, Specialization.GENERAL);
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L)))
            .thenReturn(Optional.of(serviceType));

        var bay = new ServiceBay(ServiceBayId.of(1L), DealershipId.of(1L), "Bay 1", true);
        when(serviceBayRepo.findActiveByDealershipIdWithLock(DealershipId.of(1L)))
            .thenReturn(List.of(bay));

        when(appointmentRepo.findOverlappingForBayWithLock(
            eq(ServiceBayId.of(1L)), any(), any()))
            .thenReturn(List.of());

        var tech = new Technician(TechnicianId.of(1L), DealershipId.of(1L),
            "John", Set.of(Specialization.GENERAL));
        when(technicianRepo.findByDealershipIdAndSpecialization(
            DealershipId.of(1L), Specialization.GENERAL))
            .thenReturn(List.of(tech));

        when(appointmentRepo.findOverlappingForTechnicianWithLock(
            eq(TechnicianId.of(1L)), any(), any()))
            .thenReturn(List.of());

        // When
        Result<AppointmentDto> result = handler.handle(command);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().code()).isEqualTo("Appointment.StartTimeInPast");
        verify(appointmentRepo, never()).save(any());
    }
}
