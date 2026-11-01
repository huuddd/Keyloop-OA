package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.shared.Entity;

public class ServiceBay extends Entity<ServiceBayId> {

    private ServiceBayId id;
    private DealershipId dealershipId;
    private String name;
    private boolean active;

    public ServiceBay(ServiceBayId id, DealershipId dealershipId, String name, boolean active) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
        this.active = active;
    }

    protected ServiceBay() {}

    @Override
    public ServiceBayId getId() { return id; }
    public DealershipId getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
