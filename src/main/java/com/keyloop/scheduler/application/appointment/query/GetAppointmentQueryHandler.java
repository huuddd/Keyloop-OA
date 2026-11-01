package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.EntityNotFoundException;
import com.keyloop.scheduler.application.cqrs.QueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.domain.appointment.AppointmentId;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAppointmentQueryHandler
        implements QueryHandler<GetAppointmentQuery, AppointmentDto> {

    private final AppointmentRepository appointmentRepo;

    public GetAppointmentQueryHandler(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    @Override
    public AppointmentDto handle(GetAppointmentQuery query) {
        var id = AppointmentId.of(query.appointmentId());
        return appointmentRepo.findById(id)
            .map(AppointmentDto::fromDomain)
            .orElseThrow(() -> new EntityNotFoundException(
                "Appointment not found: " + query.appointmentId()));
    }
}
