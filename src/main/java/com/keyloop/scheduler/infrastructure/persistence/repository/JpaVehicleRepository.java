package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.customer.Vehicle;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.customer.VehicleRepository;
import com.keyloop.scheduler.infrastructure.persistence.entity.VehicleJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaVehicleRepository implements VehicleRepository {

    private final SpringDataVehicleRepository springRepo;

    public JpaVehicleRepository(SpringDataVehicleRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        return springRepo.findById(id.value()).map(VehicleJpaEntity::toDomain);
    }
}
