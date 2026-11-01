package com.keyloop.scheduler.domain.customer;

import com.keyloop.scheduler.domain.shared.Entity;

public class Customer extends Entity<CustomerId> {

    private CustomerId id;
    private String name;
    private String email;
    private String phone;

    public Customer(CustomerId id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    protected Customer() {}

    @Override
    public CustomerId getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
