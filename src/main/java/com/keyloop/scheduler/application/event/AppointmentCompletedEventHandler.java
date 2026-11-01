package com.keyloop.scheduler.application.event;

import com.keyloop.scheduler.domain.appointment.event.AppointmentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCompletedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCompletedEventHandler.class);

    @EventListener
    public void handle(AppointmentCompletedEvent event) {
        log.info("Appointment completed: id={}, at={}",
            event.appointmentId(), event.completedAt());
    }
}
