package com.keyloop.scheduler.application;

import com.keyloop.scheduler.application.appointment.query.SearchAppointmentsQuery;
import com.keyloop.scheduler.application.appointment.query.SearchAppointmentsQueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.dto.PagedResult;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAppointmentsQueryHandlerTest {

    @Mock private AppointmentRepository appointmentRepo;
    @InjectMocks private SearchAppointmentsQueryHandler handler;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Test
    void delegatesToRepository() {
        var query = new SearchAppointmentsQuery(
            1L, "CONFIRMED",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
            "startTime", "ASC", 0, 10);

        var appointment = Appointment.reconstruct(
            AppointmentId.of(UUID.randomUUID()),
            CustomerId.of(1L), VehicleId.of(1L),
            ServiceBayId.of(1L), TechnicianId.of(1L), ServiceTypeId.of(1L),
            FIXED_NOW.plusHours(1), FIXED_NOW.plusHours(2),
            AppointmentStatus.CONFIRMED, FIXED_NOW, FIXED_NOW);

        when(appointmentRepo.searchAppointments(
            1L, "CONFIRMED",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
            "startTime", "ASC", 0, 10))
            .thenReturn(new AppointmentRepository.SearchResult(List.of(appointment), 1L));

        PagedResult<AppointmentDto> result = handler.handle(query);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.pageIndex()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);

        verify(appointmentRepo).searchAppointments(
            1L, "CONFIRMED",
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
            "startTime", "ASC", 0, 10);
    }
}
