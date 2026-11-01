package com.keyloop.scheduler.domain.customer;

import java.util.Optional;

public interface VehicleRepository {
    Optional<Vehicle> findById(VehicleId id);
}
