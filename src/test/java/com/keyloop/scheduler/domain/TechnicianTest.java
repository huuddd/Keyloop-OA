package com.keyloop.scheduler.domain;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.Technician;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicianTest {

    @Test
    void isQualifiedFor_withMatchingSpecialization_returnsTrue() {
        Technician tech = new Technician(
                TechnicianId.of(1L), DealershipId.of(1L), "John",
                Set.of(Specialization.GENERAL, Specialization.BRAKES));

        assertThat(tech.isQualifiedFor(Specialization.GENERAL)).isTrue();
        assertThat(tech.isQualifiedFor(Specialization.BRAKES)).isTrue();
    }

    @Test
    void isQualifiedFor_withoutMatchingSpecialization_returnsFalse() {
        Technician tech = new Technician(
                TechnicianId.of(1L), DealershipId.of(1L), "John",
                Set.of(Specialization.GENERAL, Specialization.BRAKES));

        assertThat(tech.isQualifiedFor(Specialization.ENGINE)).isFalse();
    }
}
