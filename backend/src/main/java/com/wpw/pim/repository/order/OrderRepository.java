package com.wpw.pim.repository.order;

import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o JOIN FETCH o.dealer WHERE o.dealer.id = :dealerId ORDER BY o.submittedAt DESC")
    List<Order> findByDealerIdOrderBySubmittedAtDesc(UUID dealerId);

    @Query("SELECT o FROM Order o JOIN FETCH o.dealer JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findWithItemsById(UUID id);

    @Query("SELECT DISTINCT o.dealer.id FROM Order o WHERE o.status IN :statuses")
    List<UUID> findDealerIdsWithStatuses(List<OrderStatus> statuses);

    boolean existsByDealerIdAndStatusIn(UUID dealerId, List<OrderStatus> statuses);
}
