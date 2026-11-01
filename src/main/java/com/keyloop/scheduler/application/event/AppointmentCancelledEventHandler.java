package com.keyloop.scheduler.application.event;

import com.keyloop.scheduler.domain.appointment.event.AppointmentCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCancelledEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCancelledEventHandler.class);

    @EventListener
    public void handle(AppointmentCancelledEvent event) {
        log.info("Appointment cancelled: id={}, customer={}, at={}",
            event.appointmentId(), event.customerId(), event.cancelledAt());
    }
}
