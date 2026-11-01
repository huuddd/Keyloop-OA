package com.keyloop.scheduler.domain.servicetype;

import java.util.Optional;

public interface ServiceTypeRepository {
    Optional<ServiceType> findById(ServiceTypeId id);
}
