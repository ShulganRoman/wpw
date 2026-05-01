package com.wpw.pim.repository.dealer;

import com.wpw.pim.domain.dealer.DealerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DealerContactRepository extends JpaRepository<DealerContact, UUID> {

    @Query("SELECT c FROM DealerContact c WHERE c.dealer.id = :dealerId ORDER BY c.isPrimary DESC, c.createdAt ASC")
    List<DealerContact> findByDealerId(UUID dealerId);

    @Query("SELECT c FROM DealerContact c WHERE c.dealer.id = :dealerId AND c.isPrimary = true ORDER BY c.createdAt ASC")
    java.util.Optional<DealerContact> findPrimaryByDealerId(UUID dealerId);

    void deleteByDealerId(UUID dealerId);
}
