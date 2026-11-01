package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.query.GetAvailableSlotsQuery;
import com.keyloop.scheduler.application.appointment.query.GetAvailableSlotsQueryHandler;
import com.keyloop.scheduler.application.dto.AvailableSlotDto;
import com.keyloop.scheduler.domain.appointment.Appointment;
import com.keyloop.scheduler.domain.appointment.AppointmentId;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.appointment.AppointmentStatus;
import com.keyloop.scheduler.domain.appointment.TimeSlot;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.*;
import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailableSlotsQueryHandlerTest {

    @Mock DealershipRepository dealershipRepo;
    @Mock ServiceTypeRepository serviceTypeRepo;
    @Mock ServiceBayRepository serviceBayRepo;
    @Mock TechnicianRepository technicianRepo;
    @Mock AppointmentRepository appointmentRepo;

    @InjectMocks GetAvailableSlotsQueryHandler handler;

    private Dealership dealership;
    private ServiceType oilChange;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        dealership = new Dealership(
            DealershipId.of(1L), "Test Dealership", "123 Main St",
            LocalTime.of(8, 0), LocalTime.of(17, 0));
        oilChange = new ServiceType(
            ServiceTypeId.of(1L), "Oil Change", 60, Specialization.GENERAL);
        tomorrow = LocalDate.now().plusDays(1);
    }

    @Test
    void shouldReturnAllSlotsWhenNoExistingAppointments() {
        // Given: 1 bay, 1 technician, no appointments
        // 60-min service, 08:00-17:00, 30-min granularity
        // Last valid start: 16:00 (16:00 + 60 = 17:00)
        // Total: 08:00, 08:30, ..., 16:00 = 17 slots x 1 bay = 17
        var bay = new ServiceBay(ServiceBayId.of(1L), DealershipId.of(1L), "Bay 1", true);
        var tech = new Technician(TechnicianId.of(1L), DealershipId.of(1L), "Tech 1",
            Set.of(Specialization.GENERAL));

        when(dealershipRepo.findById(DealershipId.of(1L))).thenReturn(Optional.of(dealership));
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L))).thenReturn(Optional.of(oilChange));
        when(serviceBayRepo.findActiveByDealershipId(DealershipId.of(1L))).thenReturn(List.of(bay));
        when(technicianRepo.findByDealershipIdAndSpecialization(DealershipId.of(1L), Specialization.GENERAL))
            .thenReturn(List.of(tech));
        when(appointmentRepo.findByDealershipAndDate(DealershipId.of(1L), tomorrow))
            .thenReturn(List.of());

        var query = new GetAvailableSlotsQuery(1L, 1L, tomorrow, 0, 20);
        var result = handler.handle(query);

        assertThat(result.items()).hasSize(17);
        assertThat(result.totalCount()).isEqualTo(17);
    }

    @Test
    void shouldExcludeBookedSlots() {
        // Given: 1 bay, 1 tech, 1 existing appointment at 09:00-10:00
        // 08:30, 09:00, and 09:30 starts excluded (their 60-min windows overlap with 09:00-10:00)
        // Result: 17 - 3 = 14 slots
        var bay = new ServiceBay(ServiceBayId.of(1L), DealershipId.of(1L), "Bay 1", true);
        var tech = new Technician(TechnicianId.of(1L), DealershipId.of(1L), "Tech 1",
            Set.of(Specialization.GENERAL));

        var existingAppointment = Appointment.reconstruct(
            AppointmentId.of(java.util.UUID.randomUUID()),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            LocalDateTime.of(tomorrow, LocalTime.of(9, 0)),
            LocalDateTime.of(tomorrow, LocalTime.of(10, 0)),
            AppointmentStatus.CONFIRMED,
            LocalDateTime.now(), LocalDateTime.now());

        when(dealershipRepo.findById(DealershipId.of(1L))).thenReturn(Optional.of(dealership));
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L))).thenReturn(Optional.of(oilChange));
        when(serviceBayRepo.findActiveByDealershipId(DealershipId.of(1L))).thenReturn(List.of(bay));
        when(technicianRepo.findByDealershipIdAndSpecialization(DealershipId.of(1L), Specialization.GENERAL))
            .thenReturn(List.of(tech));
        when(appointmentRepo.findByDealershipAndDate(DealershipId.of(1L), tomorrow))
            .thenReturn(List.of(existingAppointment));

        var query = new GetAvailableSlotsQuery(1L, 1L, tomorrow, 0, 20);
        var result = handler.handle(query);

        assertThat(result.items()).hasSize(14);
        assertThat(result.items()).noneMatch(s ->
            s.startTime().toLocalTime().equals(LocalTime.of(8, 30))
            || s.startTime().toLocalTime().equals(LocalTime.of(9, 0))
            || s.startTime().toLocalTime().equals(LocalTime.of(9, 30)));
    }

    @Test
    void shouldThrowWhenDealershipNotFound() {
        when(dealershipRepo.findById(DealershipId.of(999L))).thenReturn(Optional.empty());

        var query = new GetAvailableSlotsQuery(999L, 1L, tomorrow, 0, 20);

        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowWhenDateInPast() {
        when(dealershipRepo.findById(DealershipId.of(1L))).thenReturn(Optional.of(dealership));
        when(serviceTypeRepo.findById(ServiceTypeId.of(1L))).thenReturn(Optional.of(oilChange));

        var yesterday = LocalDate.now().minusDays(1);
        var query = new GetAvailableSlotsQuery(1L, 1L, yesterday, 0, 20);

        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("past");
    }
}
