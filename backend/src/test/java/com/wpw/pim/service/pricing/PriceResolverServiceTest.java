package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.web.dto.product.PriceInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link PriceResolverService}.
 * Покрывают разрешение цен для админа, dealer principal'a и UserDetails-дилера.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceResolverServiceTest {

    @Mock private PriceListRepository priceListRepo;
    @Mock private PriceListItemRepository itemRepo;
    @Mock private ProductRepository productRepo;
    @Mock private DealerRepository dealerRepo;

    @InjectMocks
    private PriceResolverService service;

    // --- helpers ---

    private Currency usd() {
        Currency c = new Currency();
        c.setCode("USD");
        c.setSymbol("$");
        return c;
    }

    private PriceList stockList(UUID id) {
        PriceList pl = new PriceList();
        pl.setId(id);
        pl.setName("Stock");
        pl.setType("stock");
        pl.setCurrency(usd());
        return pl;
    }

    private PriceList dealerList(UUID id, LocalDate validTo) {
        PriceList pl = new PriceList();
        pl.setId(id);
        pl.setType("dealer");
        pl.setCurrency(usd());
        pl.setValidTo(validTo);
        return pl;
    }

    private Product product(UUID id) {
        Product p = new Product();
        p.setId(id);
        p.setToolNo("T-1");
        return p;
    }

    private PriceListItem item(UUID plId, UUID prodId, int minQty, BigDecimal price) {
        PriceListItem it = new PriceListItem();
        it.setId(new PriceListItemId(plId, prodId, minQty));
        it.setPrice(price);
        return it;
    }

    private Authentication adminAuth() {
        UserDetails ud = User.withUsername("admin").password("x")
            .authorities(new SimpleGrantedAuthority("MANAGE_PRICES"))
            .build();
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private Authentication catalogAuth() {
        UserDetails ud = User.withUsername("cat").password("x")
            .authorities(new SimpleGrantedAuthority("MANAGE_CATALOG"))
            .build();
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    private Authentication dealerPrincipalAuth(Dealer dealer) {
        DealerPrincipal dp = new DealerPrincipal(dealer);
        return new UsernamePasswordAuthenticationToken(dp, null, dp.getAuthorities());
    }

    private Authentication dealerUserAuth(String username) {
        UserDetails ud = User.withUsername(username).password("x")
            .authorities(new SimpleGrantedAuthority("ROLE_dealer"))
            .build();
        return new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("null если auth отсутствует")
        void nullAuth() {
            assertThat(service.resolve("T-1", null)).isNull();
        }

        @Test
        @DisplayName("null если auth не аутентифицирован")
        void notAuthenticated() {
            Authentication auth = new AnonymousAuthenticationToken("k", "p",
                List.of(new SimpleGrantedAuthority("ROLE_ANON")));
            // anon is "authenticated" by default; create explicitly unauthenticated
            UsernamePasswordAuthenticationToken unauth = new UsernamePasswordAuthenticationToken("u", "p");
            // unauth.isAuthenticated() == false by default
            assertThat(service.resolve("T-1", unauth)).isNull();
        }

        @Test
        @DisplayName("null если продукт не найден")
        void productNotFound() {
            when(productRepo.findByToolNo("missing")).thenReturn(Optional.empty());
            assertThat(service.resolve("missing", adminAuth())).isNull();
        }

        @Test
        @DisplayName("admin путь: возвращает stock-цену")
        void adminStockPrice() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(plId)));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.valueOf(10))));

            PriceInfoDto dto = service.resolve("T-1", adminAuth());

            assertThat(dto).isNotNull();
            assertThat(dto.currencyCode()).isEqualTo("USD");
            assertThat(dto.tiers()).hasSize(1);
            assertThat(dto.expired()).isFalse();
        }

        @Test
        @DisplayName("admin: null если stock-лист не существует")
        void adminNoStock() {
            UUID prodId = UUID.randomUUID();
            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());

            assertThat(service.resolve("T-1", adminAuth())).isNull();
        }

        @Test
        @DisplayName("admin: null если позиция в stock не найдена")
        void adminNoItem() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(plId)));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId)).thenReturn(List.of());

            assertThat(service.resolve("T-1", adminAuth())).isNull();
        }

        @Test
        @DisplayName("admin (MANAGE_CATALOG) тоже видит stock-цену")
        void catalogAlsoSeesStock() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(plId)));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.valueOf(5))));

            assertThat(service.resolve("T-1", catalogAuth())).isNotNull();
        }

        @Test
        @DisplayName("dealer principal: возвращает dealer-цену")
        void dealerPrincipal() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(UUID.randomUUID());
            dealer.setName("D1");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            PriceList pl = dealerList(plId, LocalDate.now().plusDays(10));
            dealer.setPriceList(pl);

            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.valueOf(7))));

            PriceInfoDto dto = service.resolve("T-1", dealerPrincipalAuth(dealer));
            assertThat(dto).isNotNull();
            assertThat(dto.expired()).isFalse();
        }

        @Test
        @DisplayName("dealer principal без price list: null")
        void dealerPrincipalNoList() {
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setName("D1");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(null);

            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));

            assertThat(service.resolve("T-1", dealerPrincipalAuth(dealer))).isNull();
        }

        @Test
        @DisplayName("dealer principal: items пустые → null")
        void dealerPrincipalNoItems() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setName("D1");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(dealerList(plId, null));

            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId)).thenReturn(List.of());

            assertThat(service.resolve("T-1", dealerPrincipalAuth(dealer))).isNull();
        }

        @Test
        @DisplayName("UserDetails с ROLE_dealer: разрешает цену через username")
        void userDetailsDealer() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(UUID.randomUUID());
            dealer.setPriceList(dealerList(plId, null));

            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(dealerRepo.findByUserUsername("user1")).thenReturn(Optional.of(dealer));
            when(itemRepo.findByPriceListIdAndProductId(plId, prodId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.ONE)));

            assertThat(service.resolve("T-1", dealerUserAuth("user1"))).isNotNull();
        }

        @Test
        @DisplayName("UserDetails без ROLE_dealer: null")
        void userDetailsNoDealerRole() {
            UUID prodId = UUID.randomUUID();
            UserDetails ud = User.withUsername("u").password("x")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());

            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));

            assertThat(service.resolve("T-1", auth)).isNull();
        }

        @Test
        @DisplayName("UserDetails: dealer не найден → null")
        void userDetailsDealerNotFound() {
            UUID prodId = UUID.randomUUID();
            when(productRepo.findByToolNo("T-1")).thenReturn(Optional.of(product(prodId)));
            when(dealerRepo.findByUserUsername("ghost")).thenReturn(Optional.empty());

            assertThat(service.resolve("T-1", dealerUserAuth("ghost"))).isNull();
        }
    }

    @Nested
    @DisplayName("resolveBatch")
    class ResolveBatch {

        @Test
        @DisplayName("пустая мапа если auth = null")
        void nullAuth() {
            assertThat(service.resolveBatch(List.of(UUID.randomUUID()), null)).isEmpty();
        }

        @Test
        @DisplayName("пустая мапа если productIds пуст")
        void emptyIds() {
            assertThat(service.resolveBatch(List.of(), adminAuth())).isEmpty();
        }

        @Test
        @DisplayName("admin: возвращает stock-цены для запрошенных продуктов")
        void adminBatch() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            PriceList stock = stockList(plId);
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stock));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of(
                item(plId, prodId, 1, BigDecimal.TEN),
                item(plId, otherId, 1, BigDecimal.ONE)
            ));

            Map<UUID, PriceInfoDto> result = service.resolveBatch(List.of(prodId), adminAuth());

            assertThat(result).containsOnlyKeys(prodId);
        }

        @Test
        @DisplayName("admin: empty если stock-лист отсутствует")
        void adminBatchNoStock() {
            UUID prodId = UUID.randomUUID();
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            assertThat(service.resolveBatch(List.of(prodId), adminAuth())).isEmpty();
        }

        @Test
        @DisplayName("dealer principal: возвращает dealer-цены")
        void dealerPrincipalBatch() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setName("D");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(dealerList(plId, LocalDate.now().minusDays(1))); // expired

            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.ONE)));

            Map<UUID, PriceInfoDto> result = service.resolveBatch(
                List.of(prodId), dealerPrincipalAuth(dealer));

            assertThat(result).containsKey(prodId);
            assertThat(result.get(prodId).expired()).isTrue();
        }

        @Test
        @DisplayName("dealer principal без price list: empty map")
        void dealerPrincipalBatchNoList() {
            Dealer dealer = new Dealer();
            dealer.setName("D");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(null);
            assertThat(service.resolveBatch(List.of(UUID.randomUUID()),
                dealerPrincipalAuth(dealer))).isEmpty();
        }

        @Test
        @DisplayName("UserDetails dealer: успешно возвращает мапу")
        void userDetailsBatch() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setPriceList(dealerList(plId, null));

            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.of(dealer));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId))
                .thenReturn(List.of(item(plId, prodId, 1, BigDecimal.ONE)));

            Map<UUID, PriceInfoDto> result = service.resolveBatch(
                List.of(prodId), dealerUserAuth("u"));

            assertThat(result).containsKey(prodId);
        }

        @Test
        @DisplayName("UserDetails dealer без price list: empty")
        void userDetailsNoList() {
            Dealer dealer = new Dealer();
            dealer.setPriceList(null);
            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.of(dealer));

            assertThat(service.resolveBatch(List.of(UUID.randomUUID()),
                dealerUserAuth("u"))).isEmpty();
        }

        @Test
        @DisplayName("UserDetails dealer не найден: empty")
        void userDetailsDealerMissing() {
            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.empty());
            assertThat(service.resolveBatch(List.of(UUID.randomUUID()),
                dealerUserAuth("u"))).isEmpty();
        }

        @Test
        @DisplayName("обычный UserDetails (не dealer): empty")
        void userDetailsRegular() {
            UserDetails ud = User.withUsername("u").password("x")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            assertThat(service.resolveBatch(List.of(UUID.randomUUID()), auth)).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolvePriceListId")
    class ResolvePriceListId {

        @Test
        @DisplayName("null если auth = null")
        void nullAuth() {
            assertThat(service.resolvePriceListId(null)).isNull();
        }

        @Test
        @DisplayName("admin: возвращает stock id")
        void admin() {
            UUID id = UUID.randomUUID();
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(id)));
            assertThat(service.resolvePriceListId(adminAuth())).isEqualTo(id);
        }

        @Test
        @DisplayName("admin: null если stock отсутствует")
        void adminNoStock() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            assertThat(service.resolvePriceListId(adminAuth())).isNull();
        }

        @Test
        @DisplayName("dealer principal с list")
        void dealerPrincipal() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setName("D");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(dealerList(id, null));
            assertThat(service.resolvePriceListId(dealerPrincipalAuth(dealer))).isEqualTo(id);
        }

        @Test
        @DisplayName("dealer principal без list: null")
        void dealerPrincipalNull() {
            Dealer dealer = new Dealer();
            dealer.setName("D");
            dealer.setApiKeyHash("h");
            dealer.setActive(true);
            dealer.setPriceList(null);
            assertThat(service.resolvePriceListId(dealerPrincipalAuth(dealer))).isNull();
        }

        @Test
        @DisplayName("UserDetails dealer: возвращает id")
        void userDetailsDealer() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setPriceList(dealerList(id, null));
            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.of(dealer));
            assertThat(service.resolvePriceListId(dealerUserAuth("u"))).isEqualTo(id);
        }

        @Test
        @DisplayName("UserDetails dealer без price list: null")
        void userDetailsNoList() {
            Dealer dealer = new Dealer();
            dealer.setPriceList(null);
            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.of(dealer));
            assertThat(service.resolvePriceListId(dealerUserAuth("u"))).isNull();
        }

        @Test
        @DisplayName("UserDetails dealer не найден: null")
        void userDetailsMissing() {
            when(dealerRepo.findByUserUsername("u")).thenReturn(Optional.empty());
            assertThat(service.resolvePriceListId(dealerUserAuth("u"))).isNull();
        }

        @Test
        @DisplayName("обычный UserDetails (не dealer): null")
        void userDetailsRegular() {
            UserDetails ud = User.withUsername("u").password("x")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            assertThat(service.resolvePriceListId(auth)).isNull();
        }
    }
}
