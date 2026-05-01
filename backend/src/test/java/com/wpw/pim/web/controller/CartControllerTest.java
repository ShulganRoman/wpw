package com.wpw.pim.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import com.wpw.pim.config.SecurityConfig;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.ApiKeyAuthProvider;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.cart.CartService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.cart.AddToCartRequest;
import com.wpw.pim.web.dto.cart.CartDto;
import com.wpw.pim.web.dto.cart.CartItemDto;
import com.wpw.pim.web.dto.cart.PriceTierDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CartService cartService;
    @MockitoBean private ProductService productService;
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
        dealer.setName("dealer1");
        dealer.setActive(true);
        dealerPrincipal = new DealerPrincipal(dealer);
    }

    private CartDto emptyCart() {
        return new CartDto(List.of(), "USD", BigDecimal.ZERO, 0, List.of());
    }

    private CartDto cartWithItem(UUID productId) {
        CartItemDto item = new CartItemDto(
            productId, "T001", "Test Product", null, 2,
            new BigDecimal("10.00"), new BigDecimal("20.00"),
            List.of(new PriceTierDto(1, new BigDecimal("10.00")))
        );
        return new CartDto(List.of(item), "USD", new BigDecimal("20.00"), 1, List.of());
    }

    // ── GET /api/v1/dealer/cart ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/dealer/cart")
    class GetCart {

        @Test
        @DisplayName("возвращает корзину дилера")
        void returnsCart() throws Exception {
            when(cartService.getCart(dealerId)).thenReturn(emptyCart());

            mockMvc.perform(get("/api/v1/dealer/cart")
                    .with(user(dealerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalItems").value(0));
        }

        @Test
        @DisplayName("без аутентификации — 4xx")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/dealer/cart"))
                .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("роль ADMIN — 403")
        void adminForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/dealer/cart"))
                .andExpect(status().isForbidden());
        }
    }

    // ── POST /api/v1/dealer/cart/items ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/dealer/cart/items")
    class AddItems {

        @Test
        @DisplayName("добавляет позиции и возвращает обновлённую корзину")
        void addsItems() throws Exception {
            UUID productId = UUID.randomUUID();
            when(cartService.addItems(eq(dealerId), any())).thenReturn(cartWithItem(productId));

            AddToCartRequest req = new AddToCartRequest(List.of(productId));

            mockMvc.perform(post("/api/v1/dealer/cart/items")
                    .with(user(dealerPrincipal))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].toolNo").value("T001"));
        }

        @Test
        @DisplayName("без аутентификации — 4xx")
        void unauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/dealer/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().is4xxClientError());
        }
    }

    // ── PATCH /api/v1/dealer/cart/items/{productId} ────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/dealer/cart/items/{productId}")
    class UpdateQty {

        @Test
        @DisplayName("обновляет количество")
        void updatesQty() throws Exception {
            UUID productId = UUID.randomUUID();
            when(cartService.updateQty(dealerId, productId, 5)).thenReturn(cartWithItem(productId));

            mockMvc.perform(patch("/api/v1/dealer/cart/items/" + productId)
                    .with(user(dealerPrincipal))
                    .param("qty", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));

            verify(cartService).updateQty(dealerId, productId, 5);
        }
    }

    // ── DELETE /api/v1/dealer/cart/items/{productId} ───────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/dealer/cart/items/{productId}")
    class RemoveItem {

        @Test
        @DisplayName("удаляет позицию и возвращает корзину")
        void removesItem() throws Exception {
            UUID productId = UUID.randomUUID();
            when(cartService.removeItem(dealerId, productId)).thenReturn(emptyCart());

            mockMvc.perform(delete("/api/v1/dealer/cart/items/" + productId)
                    .with(user(dealerPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

            verify(cartService).removeItem(dealerId, productId);
        }
    }

    // ── DELETE /api/v1/dealer/cart ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/dealer/cart")
    class ClearCart {

        @Test
        @DisplayName("очищает корзину — 204")
        void clearsCart() throws Exception {
            mockMvc.perform(delete("/api/v1/dealer/cart")
                    .with(user(dealerPrincipal)))
                .andExpect(status().isNoContent());

            verify(cartService).clearCart(dealerId);
        }
    }

}
