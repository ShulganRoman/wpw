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
        @DisplayName("returns list of SKU mappings for dealer")
        void getSkuMapping_existingDealer_returnsMappings() {
            UUID dealerId = UUID.randomUUID();

            DealerSkuMapping mapping = new DealerSkuMapping();
            DealerSkuMappingId id = new DealerSkuMappingId(dealerId, "WPW-001");
            mapping.setId(id);
            mapping.setDealerSku("DEALER-SKU-001");
            mapping.setDealerBrand("BrandX");

            when(skuMappingRepo.findByDealerId(dealerId)).thenReturn(List.of(mapping));

            List<SkuMappingDto> result = dealerService.getSkuMapping(dealerId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).wpwSku()).isEqualTo("WPW-001");
            assertThat(result.get(0).dealerSku()).isEqualTo("DEALER-SKU-001");
        }

        @Test
        @DisplayName("returns empty list if no mappings")
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
        @DisplayName("creates new SKU mapping")
        void saveSkuMapping_newMapping_createsSuccessfully() {
            UUID dealerId = UUID.randomUUID();

            Dealer dealer = new Dealer();
            dealer.setId(dealerId);

            SkuMappingCreateRequest request = new SkuMappingCreateRequest("WPW-001", "MY-SKU", "BrandX");

            DealerSkuMappingId mappingId = new DealerSkuMappingId(dealerId, "WPW-001");
            when(skuMappingRepo.findById(mappingId)).thenReturn(Optional.empty());

            SkuMappingDto result = dealerService.saveSkuMapping(dealerId, request, dealer);

            assertThat(result.wpwSku()).isEqualTo("WPW-001");
            assertThat(result.dealerSku()).isEqualTo("MY-SKU");
            verify(skuMappingRepo).save(any(DealerSkuMapping.class));
        }
    }

    @Nested
    @DisplayName("getPriceList")
    class GetPriceList {

        @Test
        @DisplayName("returns dealer price list")
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
        @DisplayName("throws NOT_FOUND if price list is not assigned")
        void getPriceList_noPriceList_throws404() {
            Dealer dealer = new Dealer();
            dealer.setPriceList(null);

            assertThatThrownBy(() -> dealerService.getPriceList(dealer))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
