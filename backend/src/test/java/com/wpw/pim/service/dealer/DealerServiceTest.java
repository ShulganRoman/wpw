package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link DealerService}.
 * Покрывают SKU маппинг и работу с прайс-листами дилеров.
 */
@ExtendWith(MockitoExtension.class)
class DealerServiceTest {

    @Mock private DealerSkuMappingRepository skuMappingRepo;
    @Mock private PriceListItemRepository priceItemRepo;
    @Mock private ProductRepository productRepo;

    @InjectMocks
    private DealerService dealerService;

    @Nested
    @DisplayName("getSkuMapping")
    class GetSkuMapping {

        @Test
        @DisplayName("возвращает список SKU-маппингов для дилера")
        void getSkuMapping_existingDealer_returnsMappings() {
            UUID dealerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-001");

            DealerSkuMapping mapping = new DealerSkuMapping();
            mapping.setProduct(product);
            mapping.setDealerSku("DEALER-SKU-001");

            when(skuMappingRepo.findByDealerId(dealerId)).thenReturn(List.of(mapping));

            List<SkuMappingDto> result = dealerService.getSkuMapping(dealerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).toolNo()).isEqualTo("TOOL-001");
            assertThat(result.get(0).dealerSku()).isEqualTo("DEALER-SKU-001");
        }

        @Test
        @DisplayName("возвращает пустой список если нет маппингов")
        void getSkuMapping_noMappings_returnsEmpty() {
            UUID dealerId = UUID.randomUUID();
            when(skuMappingRepo.findByDealerId(dealerId)).thenReturn(List.of());

            assertThat(dealerService.getSkuMapping(dealerId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveSkuMapping")
    class SaveSkuMapping {

        @Test
        @DisplayName("создаёт новый SKU маппинг")
        void saveSkuMapping_newMapping_createsSuccessfully() {
            UUID dealerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-001");

            Dealer dealer = new Dealer();
            dealer.setId(dealerId);

            DealerSkuMappingId mappingId = new DealerSkuMappingId(dealerId, productId);
            SkuMappingCreateRequest request = new SkuMappingCreateRequest(productId, "MY-SKU");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(skuMappingRepo.findById(mappingId)).thenReturn(Optional.empty());

            SkuMappingDto result = dealerService.saveSkuMapping(dealerId, request, dealer);

            assertThat(result.toolNo()).isEqualTo("TOOL-001");
            assertThat(result.dealerSku()).isEqualTo("MY-SKU");
            verify(skuMappingRepo).save(any(DealerSkuMapping.class));
        }

        @Test
        @DisplayName("бросает NOT_FOUND если продукт не найден")
        void saveSkuMapping_productNotFound_throws404() {
            UUID dealerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Dealer dealer = new Dealer();

            when(productRepo.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dealerService.saveSkuMapping(dealerId,
                    new SkuMappingCreateRequest(productId, "SKU"), dealer))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("getPriceList")
    class GetPriceList {

        @Test
        @DisplayName("возвращает прайс-лист дилера")
        void getPriceList_hasPriceList_returnsPriceListDto() {
            UUID priceListId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Currency currency = new Currency();
            currency.setCode("USD");
            currency.setSymbol("$");

            PriceList priceList = new PriceList();
            priceList.setId(priceListId);
            priceList.setName("Standard");
            priceList.setCurrency(currency);

            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-001");

            PriceListItem item = new PriceListItem();
            item.setId(new PriceListItemId(priceListId, productId, 1));
            item.setProduct(product);
            item.setPrice(BigDecimal.valueOf(29.99));

            Dealer dealer = new Dealer();
            dealer.setPriceList(priceList);

            when(priceItemRepo.findByPriceListIdOrderByIdMinQtyAsc(priceListId)).thenReturn(List.of(item));

            PriceListDto result = dealerService.getPriceList(dealer);

            assertThat(result.name()).isEqualTo("Standard");
            assertThat(result.currencyCode()).isEqualTo("USD");
            assertThat(result.currencySymbol()).isEqualTo("$");
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).price()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        }

        @Test
        @DisplayName("бросает NOT_FOUND если прайс-лист не назначен")
        void getPriceList_noPriceList_throws404() {
            Dealer dealer = new Dealer();
            dealer.setPriceList(null);

            assertThatThrownBy(() -> dealerService.getPriceList(dealer))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
