package com.keyloop.scheduler.domain.appointment;

public enum AppointmentStatus {
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case CONFIRMED -> target == IN_PROGRESS
                           || target == CANCELLED
                           || target == NO_SHOW;
            case IN_PROGRESS -> target == COMPLETED
                             || target == CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
