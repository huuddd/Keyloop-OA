package com.keyloop.scheduler.application.appointment.command;

import com.keyloop.scheduler.application.cqrs.CommandHandler;
import com.keyloop.scheduler.application.port.EventPublisher;
import com.keyloop.scheduler.domain.appointment.AppointmentErrors;
import com.keyloop.scheduler.domain.appointment.AppointmentId;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.shared.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class CancelAppointmentCommandHandler
        implements CommandHandler<CancelAppointmentCommand, Void> {

    private final AppointmentRepository appointmentRepo;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public CancelAppointmentCommandHandler(
            AppointmentRepository appointmentRepo,
            EventPublisher eventPublisher,
            Clock clock) {
        this.appointmentRepo = appointmentRepo;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(CancelAppointmentCommand command) {
        var appointmentId = AppointmentId.of(command.appointmentId());
        var appointment = appointmentRepo.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return Result.failure(AppointmentErrors.notFound(appointmentId));
        }

        var cancelResult = appointment.cancel(clock);
        if (cancelResult.isFailure()) {
            return cancelResult;
        }

        appointmentRepo.save(appointment);
        eventPublisher.publishEventsFor(appointment);

        return Result.success();
    }
}
