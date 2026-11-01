package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.appointment.AppointmentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    // --- Valid transitions ---

    @Test
    void confirmed_canTransitionTo_inProgress() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    void confirmed_canTransitionTo_cancelled() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
    }

    @Test
    void confirmed_canTransitionTo_noShow() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.NO_SHOW)).isTrue();
    }

    @Test
    void inProgress_canTransitionTo_completed() {
        assertThat(AppointmentStatus.IN_PROGRESS.canTransitionTo(AppointmentStatus.COMPLETED)).isTrue();
    }

    @Test
    void inProgress_canTransitionTo_cancelled() {
        assertThat(AppointmentStatus.IN_PROGRESS.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
    }

    // --- Invalid transitions ---

    @Test
    void confirmed_cannotTransitionTo_completed() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.COMPLETED)).isFalse();
    }

    @Test
    void confirmed_cannotTransitionTo_confirmed() {
        assertThat(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.CONFIRMED)).isFalse();
    }

    @Test
    void inProgress_cannotTransitionTo_inProgress() {
        assertThat(AppointmentStatus.IN_PROGRESS.canTransitionTo(AppointmentStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void inProgress_cannotTransitionTo_noShow() {
        assertThat(AppointmentStatus.IN_PROGRESS.canTransitionTo(AppointmentStatus.NO_SHOW)).isFalse();
    }

    @Test
    void completed_cannotTransitionToAnything() {
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(AppointmentStatus.COMPLETED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void cancelled_cannotTransitionToAnything() {
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(AppointmentStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void noShow_cannotTransitionToAnything() {
        for (AppointmentStatus target : AppointmentStatus.values()) {
            assertThat(AppointmentStatus.NO_SHOW.canTransitionTo(target)).isFalse();
        }
    }

    // --- isTerminal ---

    @Test
    void completed_isTerminal() {
        assertThat(AppointmentStatus.COMPLETED.isTerminal()).isTrue();
    }

    @Test
    void cancelled_isTerminal() {
        assertThat(AppointmentStatus.CANCELLED.isTerminal()).isTrue();
    }

    @Test
    void noShow_isTerminal() {
        assertThat(AppointmentStatus.NO_SHOW.isTerminal()).isTrue();
    }

    @Test
    void confirmed_isNotTerminal() {
        assertThat(AppointmentStatus.CONFIRMED.isTerminal()).isFalse();
    }

    @Test
    void inProgress_isNotTerminal() {
        assertThat(AppointmentStatus.IN_PROGRESS.isTerminal()).isFalse();
    }
}
