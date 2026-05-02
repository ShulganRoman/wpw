package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.order.OrderStatus;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.ChangeStatusRequest;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderItemDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(AdminOrderController.class)
class AdminOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private PimUserDetailsService pimUserDetailsService;
    @MockitoBean private ApiKeyAuthProvider apiKeyAuthProvider;

    private OrderSummaryDto sampleSummary(UUID id) {
        return new OrderSummaryDto(
            id, OrderStatus.SUBMITTED, "New", "USD",
            new BigDecimal("100.00"), 1,
            OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private OrderDto sampleOrder(UUID orderId, UUID dealerId) {
        OrderItemDto item = new OrderItemDto(
            UUID.randomUUID(), "T001", "Tool 1",
            2, new BigDecimal("50.00"), new BigDecimal("100.00")
        );
        return new OrderDto(
            orderId, dealerId, "Acme LLC",
            OrderStatus.SUBMITTED, "New", "USD",
            new BigDecimal("100.00"),
            OffsetDateTime.now(), OffsetDateTime.now(),
            List.of(item)
        );
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/dealers/{dealerId}/orders -- list orders")
    void getDealerOrders() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(orderService.getAdminDealerOrders(dealerId))
            .thenReturn(List.of(sampleSummary(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].statusLabel").value("New"))
            .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/orders/{orderId} -- order details")
    void getOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        when(orderService.getAdminOrder(orderId)).thenReturn(sampleOrder(orderId, dealerId));

        mockMvc.perform(get("/api/v1/admin/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId.toString()))
            .andExpect(jsonPath("$.dealerName").value("Acme LLC"))
            .andExpect(jsonPath("$.items[0].toolNo").value("T001"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/status -- changes status")
    void changeStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        OrderDto dto = new OrderDto(
            orderId, dealerId, "Acme LLC",
            OrderStatus.CONFIRMED, "Confirmed", "USD",
            new BigDecimal("100.00"),
            OffsetDateTime.now(), OffsetDateTime.now(),
            List.of()
        );
        when(orderService.changeStatus(eq(orderId), eq(OrderStatus.CONFIRMED))).thenReturn(dto);

        ChangeStatusRequest req = new ChangeStatusRequest(OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.statusLabel").value("Confirmed"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/dealers/{dealerId}/orders/pending -- true")
    void hasPendingTrue() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(orderService.hasPendingOrders(dealerId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/orders/pending"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/dealers/{dealerId}/orders/pending -- false")
    void hasPendingFalse() throws Exception {
        UUID dealerId = UUID.randomUUID();
        when(orderService.hasPendingOrders(dealerId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/dealers/" + dealerId + "/orders/pending"))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_DEALERS")
    @DisplayName("GET /api/v1/admin/orders/pending-dealer-ids -- array of IDs")
    void pendingDealerIds() throws Exception {
        UUID id1 = UUID.randomUUID();
        when(orderService.getDealerIdsWithPendingOrders()).thenReturn(Set.of(id1));

        mockMvc.perform(get("/api/v1/admin/orders/pending-dealer-ids"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(id1.toString()));
    }

    @Test
    @DisplayName("without authentication -- 4xx")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dealers/" + UUID.randomUUID() + "/orders"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    @DisplayName("without MANAGE_DEALERS -- 403")
    void wrongAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dealers/" + UUID.randomUUID() + "/orders"))
            .andExpect(status().isForbidden());
    }
}
