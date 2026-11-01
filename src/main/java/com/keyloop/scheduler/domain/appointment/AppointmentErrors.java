package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.shared.Error;

public final class AppointmentErrors {

    private AppointmentErrors() {}

    public static final Error TECHNICIAN_NOT_QUALIFIED = new Error(
        "Appointment.TechnicianNotQualified",
        "Technician is not qualified for the requested service type");

    public static final Error START_TIME_IN_PAST = new Error(
        "Appointment.StartTimeInPast",
        "Appointment start time must be in the future");

    public static final Error ALREADY_CANCELLED = new Error(
        "Appointment.AlreadyCancelled",
        "Appointment is already cancelled");

    public static Error invalidTransition(String from, String to) {
        return new Error(
            "Appointment.InvalidTransition",
            "Cannot transition from " + from + " to " + to);
    }

    public static Error notFound(AppointmentId id) {
        return new Error("Appointment.NotFound", "Appointment not found: " + id);
    }

    public static final Error NO_BAY_AVAILABLE = new Error(
        "Appointment.NoBayAvailable",
        "No service bay available at the requested time");

    public static final Error NO_TECHNICIAN_AVAILABLE = new Error(
        "Appointment.NoTechnicianAvailable",
        "No qualified technician available at the requested time");
}
