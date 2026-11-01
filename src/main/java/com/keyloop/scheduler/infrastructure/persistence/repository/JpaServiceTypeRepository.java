package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeRepository;
import com.keyloop.scheduler.infrastructure.persistence.entity.ServiceTypeJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaServiceTypeRepository implements ServiceTypeRepository {

    private final SpringDataServiceTypeRepository springRepo;

    public JpaServiceTypeRepository(SpringDataServiceTypeRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<ServiceType> findById(ServiceTypeId id) {
        return springRepo.findById(id.value()).map(ServiceTypeJpaEntity::toDomain);
    }
}
