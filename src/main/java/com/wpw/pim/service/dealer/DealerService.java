package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealerService {

    private final DealerSkuMappingRepository skuMappingRepo;
    private final PriceListItemRepository priceItemRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public List<SkuMappingDto> getSkuMapping(UUID dealerId) {
        return skuMappingRepo.findByDealerId(dealerId).stream()
            .map(m -> new SkuMappingDto(
                m.getProduct().getId(),
                m.getProduct().getToolNo(),
                m.getDealerSku()
            )).toList();
    }

    @Transactional
    public SkuMappingDto saveSkuMapping(UUID dealerId, SkuMappingCreateRequest request, Dealer dealer) {
        Product product = productRepo.findById(request.productId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        DealerSkuMappingId id = new DealerSkuMappingId(dealerId, request.productId());
        DealerSkuMapping mapping = skuMappingRepo.findById(id).orElse(new DealerSkuMapping());
        mapping.setId(id);
        mapping.setDealer(dealer);
        mapping.setProduct(product);
        mapping.setDealerSku(request.dealerSku());
        skuMappingRepo.save(mapping);

        return new SkuMappingDto(product.getId(), product.getToolNo(), request.dealerSku());
    }

    @Transactional(readOnly = true)
    public PriceListDto getPriceList(Dealer dealer) {
        PriceList priceList = dealer.getPriceList();
        if (priceList == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No price list assigned to dealer");
        }

        List<PriceListItem> items = priceItemRepo.findByPriceListIdOrderByIdMinQtyAsc(priceList.getId());
        List<PriceListDto.PriceItemDto> itemDtos = items.stream()
            .map(i -> new PriceListDto.PriceItemDto(
                i.getProduct().getId(),
                i.getProduct().getToolNo(),
                i.getPrice(),
                i.getId().getMinQty()
            )).toList();

        return new PriceListDto(
            priceList.getId(),
            priceList.getName(),
            priceList.getCurrency().getCode(),
            priceList.getCurrency().getSymbol(),
            itemDtos
        );
    }
}
