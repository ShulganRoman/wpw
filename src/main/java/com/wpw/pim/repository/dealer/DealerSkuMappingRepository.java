package com.wpw.pim.repository.dealer;

import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DealerSkuMappingRepository extends JpaRepository<DealerSkuMapping, DealerSkuMappingId> {

    @Query("SELECT m FROM DealerSkuMapping m JOIN FETCH m.product WHERE m.dealer.id = :dealerId")
    List<DealerSkuMapping> findByDealerId(UUID dealerId);
}
