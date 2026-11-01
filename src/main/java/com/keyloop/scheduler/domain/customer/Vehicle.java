package com.keyloop.scheduler.domain.customer;

import com.keyloop.scheduler.domain.shared.Entity;

public class Vehicle extends Entity<VehicleId> {

    private VehicleId id;
    private CustomerId customerId;
    private String make;
    private String model;
    private int year;
    private String vin;

    public Vehicle(VehicleId id, CustomerId customerId, String make,
                   String model, int year, String vin) {
        this.id = id;
        this.customerId = customerId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.vin = vin;
    }

    protected Vehicle() {}

    @Override
    public VehicleId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getVin() { return vin; }
}
