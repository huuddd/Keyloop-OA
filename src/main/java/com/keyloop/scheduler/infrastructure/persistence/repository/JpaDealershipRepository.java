package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.dealership.Dealership;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.DealershipRepository;
import com.keyloop.scheduler.infrastructure.persistence.entity.DealershipJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaDealershipRepository implements DealershipRepository {

    private final SpringDataDealershipRepository springRepo;

    public JpaDealershipRepository(SpringDataDealershipRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Dealership> findById(DealershipId id) {
        return springRepo.findById(id.value()).map(DealershipJpaEntity::toDomain);
    }
}
