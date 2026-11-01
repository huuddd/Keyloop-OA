package com.keyloop.scheduler.domain.customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(CustomerId id);
}
