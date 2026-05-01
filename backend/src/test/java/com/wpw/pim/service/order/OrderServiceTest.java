package com.wpw.pim.service.order;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerContact;
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
import com.wpw.pim.web.dto.order.CheckoutResponse;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private DealerContactRepository contactRepository;
    @Mock private CartService cartService;
    @Mock private EmailService emailService;

    @InjectMocks
    private OrderService service;

    private Dealer dealer(UUID id) {
        Dealer d = new Dealer();
        d.setId(id);
        d.setName("dealer-name");
        d.setCompanyName("Acme LLC");
        return d;
    }

    private Order orderOf(UUID orderId, Dealer d) {
        Order o = new Order();
        o.setId(orderId);
        o.setDealer(d);
        o.setCurrency("USD");
        o.setTotal(new BigDecimal("100.00"));
        o.setStatus(OrderStatus.SUBMITTED);
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(o);
        item.setProductId(UUID.randomUUID());
        item.setToolNo("T001");
        item.setName("Tool 1");
        item.setQty(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        o.getItems().add(item);
        return o;
    }

    @Nested
    @DisplayName("Checkout")
    class Checkout {

        @Test
        @DisplayName("успешно создаёт заказ, очищает корзину, отправляет email")
        void success() {
            UUID dealerId = UUID.randomUUID();
            Dealer d = dealer(dealerId);

            CartItemDto cartItem = new CartItemDto(
                UUID.randomUUID(), "T001", "Tool 1", null, 2,
                new BigDecimal("50.00"), new BigDecimal("100.00"), List.of()
            );
            CartDto cart = new CartDto(List.of(cartItem), "USD", new BigDecimal("100.00"), 1, List.of());

            when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(d));
            when(cartService.getCart(dealerId)).thenReturn(cart);
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });

            CheckoutResponse response = service.checkout(dealerId);

            assertThat(response.orderId()).isNotNull();
            assertThat(response.message()).isNotBlank();
            verify(cartService).clearCart(dealerId);
            verify(emailService).sendOrderSubmittedToAdmins(any(Order.class));
        }

        @Test
        @DisplayName("пустая корзина -- 400")
        void emptyCart() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer(dealerId)));
            when(cartService.getCart(dealerId)).thenReturn(
                new CartDto(List.of(), "USD", BigDecimal.ZERO, 0, List.of())
            );

            assertThatThrownBy(() -> service.checkout(dealerId))
                .isInstanceOf(ResponseStatusException.class);
            verify(orderRepository, never()).save(any());
            verify(emailService, never()).sendOrderSubmittedToAdmins(any());
        }

        @Test
        @DisplayName("дилер не найден -- 404")
        void dealerNotFound() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.checkout(dealerId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("getDealerOrders")
    class GetDealerOrders {

        @Test
        @DisplayName("возвращает список заказов")
        void returnsList() {
            UUID dealerId = UUID.randomUUID();
            Dealer d = dealer(dealerId);
            Order o = orderOf(UUID.randomUUID(), d);

            when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(d));
            when(orderRepository.findByDealerIdOrderBySubmittedAtDesc(dealerId)).thenReturn(List.of(o));

            List<OrderSummaryDto> result = service.getDealerOrders(dealerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).statusLabel()).isEqualTo("Отправлено");
        }

        @Test
        @DisplayName("дилер не найден -- 404")
        void notFound() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDealerOrders(dealerId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("getDealerOrder")
    class GetDealerOrder {

        @Test
        @DisplayName("возвращает заказ дилера")
        void success() {
            UUID dealerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Dealer d = dealer(dealerId);
            Order o = orderOf(orderId, d);

            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(o));

            OrderDto dto = service.getDealerOrder(dealerId, orderId);

            assertThat(dto.id()).isEqualTo(orderId);
            assertThat(dto.statusLabel()).isEqualTo("Отправлено");
            assertThat(dto.items()).hasSize(1);
        }

        @Test
        @DisplayName("заказ принадлежит другому дилеру -- 403")
        void accessDenied() {
            UUID dealerId = UUID.randomUUID();
            UUID otherDealerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Order o = orderOf(orderId, dealer(otherDealerId));

            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.getDealerOrder(dealerId, orderId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("admin getters")
    class GetAdminOrders {

        @Test
        @DisplayName("getAdminDealerOrders возвращает список с админ-метками")
        void adminList() {
            UUID dealerId = UUID.randomUUID();
            Order o = orderOf(UUID.randomUUID(), dealer(dealerId));

            when(orderRepository.findByDealerIdOrderBySubmittedAtDesc(dealerId)).thenReturn(List.of(o));

            List<OrderSummaryDto> result = service.getAdminDealerOrders(dealerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).statusLabel()).isEqualTo("Новый");
        }

        @Test
        @DisplayName("getAdminOrder возвращает DTO с админ-метками")
        void adminOrder() {
            UUID orderId = UUID.randomUUID();
            UUID dealerId = UUID.randomUUID();
            Order o = orderOf(orderId, dealer(dealerId));

            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(o));

            OrderDto dto = service.getAdminOrder(orderId);

            assertThat(dto.statusLabel()).isEqualTo("Новый");
            assertThat(dto.dealerName()).isEqualTo("Acme LLC");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("обновляет статус и шлёт письмо дилеру")
        void updates() {
            UUID dealerId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Dealer d = dealer(dealerId);
            Order o = orderOf(orderId, d);

            DealerContact contact = new DealerContact();
            contact.setEmail("dealer@example.com");

            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(o));
            when(contactRepository.findPrimaryByDealerId(dealerId)).thenReturn(Optional.of(contact));

            OrderDto dto = service.changeStatus(orderId, OrderStatus.CONFIRMED);

            assertThat(dto.status()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(o);
            verify(emailService).sendStatusChangedToDealer(o, "dealer@example.com");
        }

        @Test
        @DisplayName("заказ не найден -- 404")
        void notFound() {
            UUID orderId = UUID.randomUUID();
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(orderId, OrderStatus.CONFIRMED))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("pending orders")
    class PendingOrders {

        @Test
        @DisplayName("hasPendingOrders -- true")
        void hasPendingTrue() {
            UUID dealerId = UUID.randomUUID();
            when(orderRepository.existsByDealerIdAndStatusIn(any(), any())).thenReturn(true);

            assertThat(service.hasPendingOrders(dealerId)).isTrue();
        }

        @Test
        @DisplayName("hasPendingOrders -- false")
        void hasPendingFalse() {
            UUID dealerId = UUID.randomUUID();
            when(orderRepository.existsByDealerIdAndStatusIn(any(), any())).thenReturn(false);

            assertThat(service.hasPendingOrders(dealerId)).isFalse();
        }

        @Test
        @DisplayName("getDealerIdsWithPendingOrders возвращает множество ID")
        void dealerIds() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            when(orderRepository.findDealerIdsWithStatuses(any())).thenReturn(List.of(id1, id2));

            Set<UUID> result = service.getDealerIdsWithPendingOrders();

            assertThat(result).containsExactlyInAnyOrder(id1, id2);
        }
    }
}
