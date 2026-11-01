package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, Long> {
}
