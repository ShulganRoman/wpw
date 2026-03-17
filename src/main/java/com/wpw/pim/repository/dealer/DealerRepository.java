package com.wpw.pim.repository.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {
    List<Dealer> findAllByIsActiveTrue();
}
