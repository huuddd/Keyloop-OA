package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBay;
import com.keyloop.scheduler.domain.dealership.ServiceBayRepository;
import com.keyloop.scheduler.infrastructure.persistence.entity.ServiceBayJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaServiceBayRepository implements ServiceBayRepository {

    private final SpringDataServiceBayRepository springRepo;

    public JpaServiceBayRepository(SpringDataServiceBayRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<ServiceBay> findActiveByDealershipId(DealershipId dealershipId) {
        return springRepo.findByDealershipIdAndIsActiveTrue(dealershipId.value()).stream()
            .map(ServiceBayJpaEntity::toDomain).toList();
    }
}
