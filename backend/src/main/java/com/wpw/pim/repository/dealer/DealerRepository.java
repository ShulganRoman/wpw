package com.wpw.pim.repository.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {
    List<Dealer> findAllByIsActiveTrue();

    @Query("SELECT d FROM Dealer d WHERE d.user.username = :username")
    Optional<Dealer> findByUserUsername(String username);
}
