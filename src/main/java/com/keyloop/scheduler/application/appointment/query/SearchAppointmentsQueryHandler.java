package com.keyloop.scheduler.application.appointment.query;

import com.keyloop.scheduler.application.cqrs.QueryHandler;
import com.keyloop.scheduler.application.dto.AppointmentDto;
import com.keyloop.scheduler.application.dto.PagedResult;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchAppointmentsQueryHandler
        implements QueryHandler<SearchAppointmentsQuery, PagedResult<AppointmentDto>> {

    private final AppointmentRepository appointmentRepo;

    public SearchAppointmentsQueryHandler(AppointmentRepository appointmentRepo) {
        this.appointmentRepo = appointmentRepo;
    }

    @Override
    public PagedResult<AppointmentDto> handle(SearchAppointmentsQuery query) {
        var result = appointmentRepo.searchAppointments(
            query.customerId(), query.status(),
            query.dateFrom(), query.dateTo(),
            query.sortBy(), query.sortDirection(),
            query.pageIndex(), query.pageSize());

        var dtos = result.items().stream()
            .map(AppointmentDto::fromDomain).toList();

        return new PagedResult<>(dtos, result.totalCount(),
            query.pageIndex(), query.pageSize());
    }
}
