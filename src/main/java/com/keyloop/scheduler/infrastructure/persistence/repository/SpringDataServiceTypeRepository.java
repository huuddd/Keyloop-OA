package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.ServiceTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataServiceTypeRepository extends JpaRepository<ServiceTypeJpaEntity, Long> {
}
