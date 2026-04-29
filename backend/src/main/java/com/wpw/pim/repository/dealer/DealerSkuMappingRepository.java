package com.wpw.pim.repository.dealer;

import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DealerSkuMappingRepository extends JpaRepository<DealerSkuMapping, DealerSkuMappingId> {

    @Query("SELECT m FROM DealerSkuMapping m WHERE m.dealer.id = :dealerId ORDER BY m.id.wpwSku")
    List<DealerSkuMapping> findByDealerId(UUID dealerId);

    @Query("SELECT m FROM DealerSkuMapping m WHERE m.dealer.id = :dealerId AND m.id.wpwSku IN :wpwSkus")
    List<DealerSkuMapping> findByDealerIdAndWpwSkuIn(UUID dealerId, Collection<String> wpwSkus);

    @Modifying
    @Query("DELETE FROM DealerSkuMapping m WHERE m.dealer.id = :dealerId")
    void deleteByDealerId(UUID dealerId);
}
