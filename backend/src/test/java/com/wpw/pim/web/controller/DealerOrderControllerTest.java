package com.wpw.pim.web.controller;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.order.OrderStatus;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.CheckoutResponse;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(DealerOrderController.class)
class DealerOrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderService orderService;
    @MockitoBean private DealerRepository dealerRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private UUID dealerId;
    private Dealer dealer;
    private DealerPrincipal dealerPrincipal;

    @BeforeEach
    void setUp() {
        dealerId = UUID.randomUUID();
        dealer = new Dealer();
        dealer.setId(dealerId);
        dealer.setName("dealer-1");
        dealer.setActive(true);
        dealerPrincipal = new DealerPrincipal(dealer);
    }

    @Test
    @DisplayName("POST /api/v1/dealer/cart/checkout -- 200 с orderId")
    void checkout() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.checkout(dealerId)).thenReturn(new CheckoutResponse(orderId, "Заказ оформлен"));

        mockMvc.perform(post("/api/v1/dealer/cart/checkout")
                .with(user(dealerPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.message").value("Заказ оформлен"));
    }

    @Test
    @DisplayName("GET /api/v1/dealer/orders -- список заказов")
    void myOrders() throws Exception {
        OrderSummaryDto summary = new OrderSummaryDto(
            UUID.randomUUID(), OrderStatus.SUBMITTED, "Отправлено", "USD",
            new BigDecimal("100.00"), 1,
            OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(orderService.getDealerOrders(dealerId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/dealer/orders")
                .with(user(dealerPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].statusLabel").value("Отправлено"));
    }

    @Test
    @DisplayName("GET /api/v1/dealer/orders/{orderId} -- детали с dealerName")
    void myOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderDto dto = new OrderDto(
            orderId, dealerId, "Acme LLC",
            OrderStatus.SUBMITTED, "Отправлено", "USD",
            new BigDecimal("100.00"),
            OffsetDateTime.now(), OffsetDateTime.now(),
            List.of()
        );
        when(orderService.getDealerOrder(dealerId, orderId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/dealer/orders/" + orderId)
                .with(user(dealerPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dealerName").value("Acme LLC"))
            .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    @DisplayName("без аутентификации -- 4xx")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dealer/orders"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("роль ADMIN -- 403")
    void adminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/dealer/orders"))
            .andExpect(status().isForbidden());
    }
}
