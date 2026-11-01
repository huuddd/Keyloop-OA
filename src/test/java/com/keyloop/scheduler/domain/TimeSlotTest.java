package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.appointment.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 15, 9, 0);

    // --- overlapsWith ---

    @Test
    void overlappingSlots_returnsTrue() {
        TimeSlot slot1 = new TimeSlot(BASE, BASE.plusHours(2));           // 09:00-11:00
        TimeSlot slot2 = new TimeSlot(BASE.plusHours(1), BASE.plusHours(3)); // 10:00-12:00
        assertThat(slot1.overlapsWith(slot2)).isTrue();
        assertThat(slot2.overlapsWith(slot1)).isTrue();
    }

    @Test
    void nonOverlappingSlots_returnsFalse() {
        TimeSlot slot1 = new TimeSlot(BASE, BASE.plusHours(1));           // 09:00-10:00
        TimeSlot slot2 = new TimeSlot(BASE.plusHours(2), BASE.plusHours(3)); // 11:00-12:00
        assertThat(slot1.overlapsWith(slot2)).isFalse();
        assertThat(slot2.overlapsWith(slot1)).isFalse();
    }

    @Test
    void adjacentSlots_doNotOverlap() {
        TimeSlot slot1 = new TimeSlot(BASE, BASE.plusHours(1));           // 09:00-10:00
        TimeSlot slot2 = new TimeSlot(BASE.plusHours(1), BASE.plusHours(2)); // 10:00-11:00
        assertThat(slot1.overlapsWith(slot2)).isFalse();
        assertThat(slot2.overlapsWith(slot1)).isFalse();
    }

    @Test
    void containedSlot_overlaps() {
        TimeSlot outer = new TimeSlot(BASE, BASE.plusHours(4));            // 09:00-13:00
        TimeSlot inner = new TimeSlot(BASE.plusHours(1), BASE.plusHours(2)); // 10:00-11:00
        assertThat(outer.overlapsWith(inner)).isTrue();
        assertThat(inner.overlapsWith(outer)).isTrue();
    }

    @Test
    void exactSameSlot_overlaps() {
        TimeSlot slot1 = new TimeSlot(BASE, BASE.plusHours(1));
        TimeSlot slot2 = new TimeSlot(BASE, BASE.plusHours(1));
        assertThat(slot1.overlapsWith(slot2)).isTrue();
    }

    // --- Validation ---

    @Test
    void endTimeBeforeStartTime_throwsException() {
        assertThatThrownBy(() -> new TimeSlot(BASE.plusHours(1), BASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void endTimeEqualsStartTime_throwsException() {
        assertThatThrownBy(() -> new TimeSlot(BASE, BASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void nullStartTime_throwsException() {
        assertThatThrownBy(() -> new TimeSlot(null, BASE.plusHours(1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullEndTime_throwsException() {
        assertThatThrownBy(() -> new TimeSlot(BASE, null))
                .isInstanceOf(NullPointerException.class);
    }

    // --- Factory and utility ---

    @Test
    void of_createsSlotWithDuration() {
        TimeSlot slot = TimeSlot.of(BASE, 60);
        assertThat(slot.startTime()).isEqualTo(BASE);
        assertThat(slot.endTime()).isEqualTo(BASE.plusMinutes(60));
    }

    @Test
    void durationMinutes_returnsCorrectDuration() {
        TimeSlot slot = new TimeSlot(BASE, BASE.plusMinutes(90));
        assertThat(slot.durationMinutes()).isEqualTo(90);
    }
}
