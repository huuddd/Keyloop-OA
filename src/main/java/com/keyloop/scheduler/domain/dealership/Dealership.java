package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Dealership extends Entity<DealershipId> {

    private DealershipId id;
    private String name;
    private String address;
    private LocalTime operatingStart;
    private LocalTime operatingEnd;

    public Dealership(DealershipId id, String name, String address,
                      LocalTime operatingStart, LocalTime operatingEnd) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.operatingStart = operatingStart;
        this.operatingEnd = operatingEnd;
    }

    protected Dealership() {}

    /**
     * Generate all possible appointment start times for a day.
     * Uses 30-minute slot granularity.
     * The last slot must end at or before operatingEnd.
     *
     * Example: operating 08:00-17:00, duration 90 min, granularity 30 min
     * -> [08:00, 08:30, 09:00, ..., 15:30] (15:30 + 90 = 17:00 <= closing)
     */
    public List<LocalTime> generatePossibleStartTimes(int durationMinutes) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = operatingStart;
        int granularity = 30;

        while (!current.plusMinutes(durationMinutes).isAfter(operatingEnd)) {
            slots.add(current);
            current = current.plusMinutes(granularity);
        }
        return slots;
    }

    @Override
    public DealershipId getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getOperatingStart() { return operatingStart; }
    public LocalTime getOperatingEnd() { return operatingEnd; }
}
