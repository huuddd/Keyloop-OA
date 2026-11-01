package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.QueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.customer.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListAppointmentsQueryHandler
        implements QueryHandler<ListAppointmentsQuery, List<AppointmentDto>> {

    private final AppointmentRepository appointmentRepo;

    public ListAppointmentsQueryHandler(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    @Override
    public List<AppointmentDto> handle(ListAppointmentsQuery query) {
        var customerId = CustomerId.of(query.customerId());
        return appointmentRepo.findByCustomerId(customerId).stream()
            .map(AppointmentDto::fromDomain)
            .toList();
    }
}
