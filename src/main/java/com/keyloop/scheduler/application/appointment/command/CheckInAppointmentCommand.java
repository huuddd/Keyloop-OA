package com.keyloop.scheduler.application.appointment.command;

import com.keyloop.scheduler.application.cqrs.Command;

import java.util.UUID;

public record CheckInAppointmentCommand(UUID appointmentId) implements Command<Void> {}
