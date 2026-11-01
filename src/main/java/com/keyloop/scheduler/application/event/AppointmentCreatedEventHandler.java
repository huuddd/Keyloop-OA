package com.keyloop.scheduler.application.event;

import com.keyloop.scheduler.domain.appointment.event.AppointmentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCreatedEventHandler.class);

    @EventListener
    public void handle(AppointmentCreatedEvent event) {
        log.info("Appointment created: id={}, bay={}, tech={}, time={}-{}",
            event.appointmentId(), event.serviceBayId(),
            event.technicianId(), event.startTime(), event.endTime());
    }
}
