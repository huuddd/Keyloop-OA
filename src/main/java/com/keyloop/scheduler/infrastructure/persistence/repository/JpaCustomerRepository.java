package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.customer.Customer;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.CustomerRepository;
import com.keyloop.scheduler.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerRepository springRepo;

    public JpaCustomerRepository(SpringDataCustomerRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return springRepo.findById(id.value()).map(CustomerJpaEntity::toDomain);
    }
}
