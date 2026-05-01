package com.wpw.pim.service.order;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderItem;
import com.wpw.pim.domain.order.OrderStatus;
import com.wpw.pim.repository.dealer.DealerContactRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.order.OrderRepository;
import com.wpw.pim.service.cart.CartService;
import com.wpw.pim.service.email.EmailService;
import com.wpw.pim.web.dto.cart.CartDto;
import com.wpw.pim.web.dto.cart.CartItemDto;
import com.wpw.pim.web.dto.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DealerRepository dealerRepository;
    private final DealerContactRepository contactRepository;
    private final CartService cartService;
    private final EmailService emailService;

    @Transactional
    public CheckoutResponse checkout(UUID dealerId) {
        Dealer dealer = loadDealer(dealerId);
        CartDto cart = cartService.getCart(dealerId);

        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = new Order();
        order.setDealer(dealer);
        order.setCurrency(cart.currency());
        order.setTotal(cart.total());

        for (CartItemDto ci : cart.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(ci.productId());
            item.setToolNo(ci.toolNo());
            item.setName(ci.name());
            item.setQty(ci.qty());
            item.setUnitPrice(ci.unitPrice());
            item.setLineTotal(ci.lineTotal());
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        cartService.clearCart(dealerId);

        emailService.sendOrderSubmittedToAdmins(saved);

        return new CheckoutResponse(saved.getId(), "Order placed successfully");
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getDealerOrders(UUID dealerId) {
        loadDealer(dealerId);
        return orderRepository.findByDealerIdOrderBySubmittedAtDesc(dealerId)
            .stream().map(o -> toSummary(o, false))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getDealerOrder(UUID dealerId, UUID orderId) {
        Order order = loadOrderWithItems(orderId);
        if (!order.getDealer().getId().equals(dealerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toDto(order, false);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getAdminDealerOrders(UUID dealerId) {
        return orderRepository.findByDealerIdOrderBySubmittedAtDesc(dealerId)
            .stream().map(o -> toSummary(o, true))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getAdminOrder(UUID orderId) {
        return toDto(loadOrderWithItems(orderId), true);
    }

    @Transactional
    public OrderDto changeStatus(UUID orderId, OrderStatus newStatus) {
        Order order = loadOrderWithItems(orderId);
        order.setStatus(newStatus);
        order.setUpdatedAt(java.time.OffsetDateTime.now());
        orderRepository.save(order);

        String dealerEmail = resolveDealerEmail(order.getDealer());
        emailService.sendStatusChangedToDealer(order, dealerEmail);

        return toDto(order, true);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingOrders(UUID dealerId) {
        return orderRepository.existsByDealerIdAndStatusIn(
            dealerId, List.of(OrderStatus.SUBMITTED, OrderStatus.IN_PROCESSING));
    }

    @Transactional(readOnly = true)
    public Set<UUID> getDealerIdsWithPendingOrders() {
        return Set.copyOf(orderRepository.findDealerIdsWithStatuses(
            List.of(OrderStatus.SUBMITTED, OrderStatus.IN_PROCESSING)));
    }

    // ── private helpers ──────────────────────────────────────────────────────────

    private Dealer loadDealer(UUID dealerId) {
        return dealerRepository.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found"));
    }

    private Order loadOrderWithItems(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }

    private String resolveDealerEmail(Dealer dealer) {
        return contactRepository.findPrimaryByDealerId(dealer.getId())
            .map(c -> c.getEmail())
            .orElse(null);
    }

    private OrderSummaryDto toSummary(Order o, boolean adminView) {
        return new OrderSummaryDto(
            o.getId(),
            o.getStatus(),
            statusLabel(o.getStatus(), adminView),
            o.getCurrency(),
            o.getTotal(),
            o.getItems().size(),
            o.getSubmittedAt(),
            o.getUpdatedAt()
        );
    }

    private OrderDto toDto(Order o, boolean adminView) {
        List<OrderItemDto> items = o.getItems().stream()
            .map(i -> new OrderItemDto(i.getProductId(), i.getToolNo(), i.getName(),
                i.getQty(), i.getUnitPrice(), i.getLineTotal()))
            .toList();

        Dealer d = o.getDealer();
        return new OrderDto(
            o.getId(),
            d.getId(),
            d.getCompanyName() != null ? d.getCompanyName() : d.getName(),
            o.getStatus(),
            statusLabel(o.getStatus(), adminView),
            o.getCurrency(),
            o.getTotal(),
            o.getSubmittedAt(),
            o.getUpdatedAt(),
            items
        );
    }

    private String statusLabel(OrderStatus status, boolean adminView) {
        return switch (status) {
            case SUBMITTED     -> adminView ? "New"          : "Submitted";
            case IN_PROCESSING -> "In Processing";
            case CONFIRMED     -> "Confirmed";
            case REJECTED      -> "Rejected";
        };
    }
}
