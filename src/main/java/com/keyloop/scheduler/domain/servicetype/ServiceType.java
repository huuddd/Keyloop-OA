package com.keyloop.scheduler.domain.servicetype;

import com.keyloop.scheduler.domain.shared.Entity;

public class ServiceType extends Entity<ServiceTypeId> {

    private ServiceTypeId id;
    private String name;
    private int durationMinutes;
    private Specialization requiredSpecialization;

    public ServiceType(ServiceTypeId id, String name, int durationMinutes,
                       Specialization requiredSpecialization) {
        this.id = id;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredSpecialization = requiredSpecialization;
    }

    protected ServiceType() {}

    @Override
    public ServiceTypeId getId() { return id; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public Specialization getRequiredSpecialization() { return requiredSpecialization; }
}
