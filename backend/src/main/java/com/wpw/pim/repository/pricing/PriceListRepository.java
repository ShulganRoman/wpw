package com.wpw.pim.repository.pricing;

import com.wpw.pim.domain.pricing.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PriceListRepository extends JpaRepository<PriceList, UUID> {
    Optional<PriceList> findFirstByType(String type);
}
