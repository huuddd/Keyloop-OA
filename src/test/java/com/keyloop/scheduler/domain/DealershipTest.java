package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.dealership.Dealership;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DealershipTest {

    private Dealership dealership(LocalTime start, LocalTime end) {
        return new Dealership(
                DealershipId.of(1L), "Test Dealer", "123 Main St", start, end);
    }

    @Test
    void generatePossibleStartTimes_60minService_returns9Slots() {
        Dealership d = dealership(LocalTime.of(8, 0), LocalTime.of(17, 0));
        List<LocalTime> slots = d.generatePossibleStartTimes(60);

        // 08:00, 08:30, 09:00, ..., 16:00 => last slot 16:00+60=17:00 <= 17:00
        assertThat(slots).hasSize(17);
        assertThat(slots.get(0)).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(slots.size() - 1)).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void generatePossibleStartTimes_90minService_returns16Slots() {
        Dealership d = dealership(LocalTime.of(8, 0), LocalTime.of(17, 0));
        List<LocalTime> slots = d.generatePossibleStartTimes(90);

        // 08:00, 08:30, ..., 15:30 => last slot 15:30+90=17:00 <= 17:00
        assertThat(slots).hasSize(16);
        assertThat(slots.get(0)).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(slots.size() - 1)).isEqualTo(LocalTime.of(15, 30));
    }

    @Test
    void generatePossibleStartTimes_durationEqualsFullWindow_returnsSingleSlot() {
        // Operating 08:00-17:00 = 540 minutes; duration = 540
        Dealership d = dealership(LocalTime.of(8, 0), LocalTime.of(17, 0));
        List<LocalTime> slots = d.generatePossibleStartTimes(540);

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0)).isEqualTo(LocalTime.of(8, 0));
    }
}
