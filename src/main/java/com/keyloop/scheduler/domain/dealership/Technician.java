package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;
import com.keyloop.scheduler.domain.servicetype.Specialization;

import java.util.Collections;
import java.util.Set;

public class Technician extends Entity<TechnicianId> {

    private TechnicianId id;
    private DealershipId dealershipId;
    private String name;
    private Set<Specialization> specializations;

    public Technician(TechnicianId id, DealershipId dealershipId, String name,
                      Set<Specialization> specializations) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.specializations = specializations;
    }

    protected Technician() {}

    /**
     * Business rule: can this technician handle the given service type?
     */
    public boolean isQualifiedFor(Specialization required) {
        return specializations.contains(required);
    }

    @Override
    public TechnicianId getId() { return id; }
    public DealershipId getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public Set<Specialization> getSpecializations() {
        return Collections.unmodifiableSet(specializations);
    }
}
