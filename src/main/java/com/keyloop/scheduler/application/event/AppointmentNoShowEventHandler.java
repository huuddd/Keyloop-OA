package com.keyloop.scheduler.application.event;

import com.keyloop.scheduler.domain.appointment.event.AppointmentNoShowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNoShowEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNoShowEventHandler.class);

    @EventListener
    public void handle(AppointmentNoShowEvent event) {
        log.info("Appointment no-show: id={}, customer={}, at={}",
            event.appointmentId(), event.customerId(), event.markedAt());
    }
}
