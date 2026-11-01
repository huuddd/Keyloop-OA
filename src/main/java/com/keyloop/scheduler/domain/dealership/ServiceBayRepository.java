package com.keyloop.scheduler.domain.dealership;

import java.util.List;

public interface ServiceBayRepository {
    List<ServiceBay> findActiveByDealershipId(DealershipId dealershipId);
}
