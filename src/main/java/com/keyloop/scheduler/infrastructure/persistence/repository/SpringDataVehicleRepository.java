package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, Long> {
}
