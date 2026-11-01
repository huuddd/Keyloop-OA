package com.keyloop.scheduler.application.event;

import com.keyloop.scheduler.domain.appointment.event.AppointmentCheckedInEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCheckedInEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCheckedInEventHandler.class);

    @EventListener
    public void handle(AppointmentCheckedInEvent event) {
        log.info("Appointment checked in: id={}, at={}",
            event.appointmentId(), event.checkedInAt());
    }
}
