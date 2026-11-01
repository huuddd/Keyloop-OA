package com.keyloop.scheduler.domain.dealership;

import java.util.Optional;

public interface DealershipRepository {
    Optional<Dealership> findById(DealershipId id);
}
