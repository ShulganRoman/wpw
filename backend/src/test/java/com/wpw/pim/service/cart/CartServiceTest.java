package com.wpw.pim.service.cart;

import com.wpw.pim.domain.cart.CartItem;
import com.wpw.pim.domain.cart.CartItemId;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import com.wpw.pim.repository.cart.CartItemRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.cart.CartDto;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PriceListItemRepository priceListItemRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private DealerRepository dealerRepository;

    @InjectMocks
    private CartService cartService;

    private UUID dealerId;
    private UUID productId;
    private Dealer dealer;
    private Product product;

    @BeforeEach
    void setUp() {
        dealerId  = UUID.randomUUID();
        productId = UUID.randomUUID();

        dealer = new Dealer();
        dealer.setId(dealerId);

        product = new Product();
        product.setId(productId);
        product.setToolNo("T001");
        product.setStatus(ProductStatus.active);

        ProductTranslation tr = new ProductTranslation();
        tr.setId(new ProductTranslationId(productId, "en"));
        tr.setName("Test Product");
        product.setTranslations(List.of(tr));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private CartItem cartItem(int qty) {
        CartItem ci = new CartItem(dealer, product, qty);
        ci.setId(new CartItemId(dealerId, productId));
        return ci;
    }

    private void stubDealerFound() {
        when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer));
    }

    private void stubEmptyCart() {
        when(cartItemRepository.findByDealerIdWithProduct(dealerId)).thenReturn(List.of());
        when(mediaFileRepository.findByProductIds(any())).thenReturn(List.of());
    }

    // ── getCart ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("404 if dealer not found")
        void dealerNotFound() {
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> cartService.getCart(dealerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Dealer not found");
        }

        @Test
        @DisplayName("returns empty cart for new dealer")
        void emptyCart() {
            stubDealerFound();
            stubEmptyCart();

            CartDto dto = cartService.getCart(dealerId);

            assertThat(dto.items()).isEmpty();
            assertThat(dto.totalItems()).isZero();
            assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("returns USD when dealer has no price list")
        void defaultCurrencyUsd() {
            stubDealerFound();
            stubEmptyCart();

            CartDto dto = cartService.getCart(dealerId);
            assertThat(dto.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("takes currency from dealer price list")
        void currencyFromPriceList() {
            Currency eur = new Currency();
            eur.setCode("EUR");
            PriceList pl = new PriceList();
            pl.setId(UUID.randomUUID());
            pl.setCurrency(eur);
            dealer.setPriceList(pl);

            stubDealerFound();
            stubEmptyCart();
            when(priceListItemRepository.findByPriceListIdAndProductIds(eq(pl.getId()), any()))
                .thenReturn(List.of());

            CartDto dto = cartService.getCart(dealerId);
            assertThat(dto.currency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("returns item with name from English translation")
        void itemWithTranslatedName() {
            stubDealerFound();
            when(cartItemRepository.findByDealerIdWithProduct(dealerId)).thenReturn(List.of(cartItem(2)));
            when(mediaFileRepository.findByProductIds(List.of(productId))).thenReturn(List.of());

            CartDto dto = cartService.getCart(dealerId);

            assertThat(dto.items()).hasSize(1);
            assertThat(dto.items().get(0).name()).isEqualTo("Test Product");
            assertThat(dto.items().get(0).qty()).isEqualTo(2);
        }

        @Test
        @DisplayName("inactive product goes into removedToolNos and is excluded from items")
        void inactiveProductExcluded() {
            product.setStatus(ProductStatus.discontinued);
            stubDealerFound();
            when(cartItemRepository.findByDealerIdWithProduct(dealerId)).thenReturn(List.of(cartItem(1)));
            when(mediaFileRepository.findByProductIds(List.of(productId))).thenReturn(List.of());

            CartDto dto = cartService.getCart(dealerId);

            assertThat(dto.items()).isEmpty();
            assertThat(dto.removedToolNos()).contains("T001");
        }

        @Test
        @DisplayName("calculates unitPrice and lineTotal by active price level")
        void priceCalculation() {
            UUID plId = UUID.randomUUID();
            PriceList pl = new PriceList();
            pl.setId(plId);
            dealer.setPriceList(pl);

            PriceListItem tier1 = new PriceListItem();
            tier1.setId(new PriceListItemId(plId, productId, 1));
            tier1.setPrice(new BigDecimal("10.00"));
            tier1.setProduct(product);

            PriceListItem tier5 = new PriceListItem();
            tier5.setId(new PriceListItemId(plId, productId, 5));
            tier5.setPrice(new BigDecimal("8.00"));
            tier5.setProduct(product);

            stubDealerFound();
            when(cartItemRepository.findByDealerIdWithProduct(dealerId)).thenReturn(List.of(cartItem(5)));
            when(mediaFileRepository.findByProductIds(List.of(productId))).thenReturn(List.of());
            when(priceListItemRepository.findByPriceListIdAndProductIds(eq(plId), any()))
                .thenReturn(List.of(tier1, tier5));

            CartDto dto = cartService.getCart(dealerId);

            assertThat(dto.items().get(0).unitPrice()).isEqualByComparingTo("8.00");
            assertThat(dto.items().get(0).lineTotal()).isEqualByComparingTo("40.00");
            assertThat(dto.total()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("unitPrice null if price list not assigned")
        void noPriceListGivesNullPrice() {
            stubDealerFound();
            when(cartItemRepository.findByDealerIdWithProduct(dealerId)).thenReturn(List.of(cartItem(1)));
            when(mediaFileRepository.findByProductIds(List.of(productId))).thenReturn(List.of());

            CartDto dto = cartService.getCart(dealerId);

            assertThat(dto.items().get(0).unitPrice()).isNull();
            assertThat(dto.items().get(0).lineTotal()).isNull();
            assertThat(dto.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ── addItems ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addItems")
    class AddItems {

        @Test
        @DisplayName("creates new cart item if not in cart")
        void addsNewItem() {
            stubDealerFound();
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(cartItemRepository.findById(new CartItemId(dealerId, productId))).thenReturn(Optional.empty());
            stubEmptyCart();

            cartService.addItems(dealerId, List.of(productId));

            verify(cartItemRepository).save(argThat(ci -> ci.getQty() == 1));
        }

        @Test
        @DisplayName("increments qty if item already in cart")
        void incrementsExistingQty() {
            CartItem existing = cartItem(3);
            stubDealerFound();
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(cartItemRepository.findById(new CartItemId(dealerId, productId))).thenReturn(Optional.of(existing));
            stubEmptyCart();

            cartService.addItems(dealerId, List.of(productId));

            assertThat(existing.getQty()).isEqualTo(4);
            verify(cartItemRepository).save(existing);
        }

        @Test
        @DisplayName("404 if product not found")
        void productNotFound() {
            stubDealerFound();
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItems(dealerId, List.of(productId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("404 if dealer not found")
        void dealerNotFound() {
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> cartService.addItems(dealerId, List.of(productId)))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    // ── addByFilter ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addByFilter")
    class AddByFilter {

        @Test
        @DisplayName("returns cart unchanged if list is empty")
        void emptyList() {
            stubDealerFound();
            stubEmptyCart();

            CartDto dto = cartService.addByFilter(dealerId, List.of());

            verify(cartItemRepository, never()).saveAll(any());
            assertThat(dto.items()).isEmpty();
        }

        @Test
        @DisplayName("skips products already in cart")
        void skipsAlreadyInCart() {
            stubDealerFound();
            when(cartItemRepository.findProductIdsByDealerId(dealerId)).thenReturn(List.of(productId));
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
            stubEmptyCart();

            cartService.addByFilter(dealerId, List.of(productId));

            verify(cartItemRepository, never()).saveAll(argThat(list ->
                ((List<?>) list).stream().anyMatch(ci ->
                    ((CartItem) ci).getProduct().getId().equals(productId))));
        }

        @Test
        @DisplayName("skips inactive products")
        void skipsInactiveProducts() {
            product.setStatus(ProductStatus.discontinued);
            UUID otherId = UUID.randomUUID();
            stubDealerFound();
            when(cartItemRepository.findProductIdsByDealerId(dealerId)).thenReturn(List.of());
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
            stubEmptyCart();

            cartService.addByFilter(dealerId, List.of(productId));

            verify(cartItemRepository, never()).saveAll(argThat(list -> !((List<?>) list).isEmpty()));
        }

        @Test
        @DisplayName("saves new active products")
        void savesNewActiveProducts() {
            stubDealerFound();
            when(cartItemRepository.findProductIdsByDealerId(dealerId)).thenReturn(List.of());
            when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
            stubEmptyCart();

            cartService.addByFilter(dealerId, List.of(productId));

            verify(cartItemRepository).saveAll(argThat(list -> !((List<?>) list).isEmpty()));
        }
    }

    // ── updateQty ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateQty")
    class UpdateQty {

        @Test
        @DisplayName("qty <= 0 removes item")
        void zeroQtyRemovesItem() {
            stubDealerFound();
            stubEmptyCart();

            cartService.updateQty(dealerId, productId, 0);

            verify(cartItemRepository).deleteByDealerIdAndProductId(dealerId, productId);
        }

        @Test
        @DisplayName("updates qty for existing item")
        void updatesQty() {
            CartItem existing = cartItem(1);
            when(dealerRepository.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(cartItemRepository.findById(new CartItemId(dealerId, productId)))
                .thenReturn(Optional.of(existing));
            stubEmptyCart();

            cartService.updateQty(dealerId, productId, 7);

            assertThat(existing.getQty()).isEqualTo(7);
            verify(cartItemRepository).save(existing);
        }

        @Test
        @DisplayName("404 if item not found")
        void itemNotFound() {
            stubDealerFound();
            when(cartItemRepository.findById(any(CartItemId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateQty(dealerId, productId, 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Item not in cart");
        }
    }

    // ── removeItem ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("removes item and returns cart")
        void removesItem() {
            stubDealerFound();
            stubEmptyCart();

            cartService.removeItem(dealerId, productId);

            verify(cartItemRepository).deleteByDealerIdAndProductId(dealerId, productId);
        }
    }

    // ── clearCart ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clearCart")
    class ClearCartTest {

        @Test
        @DisplayName("removes all dealer items")
        void clearsAll() {
            cartService.clearCart(dealerId);
            verify(cartItemRepository).deleteByDealerId(dealerId);
        }
    }
}
