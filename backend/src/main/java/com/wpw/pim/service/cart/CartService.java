package com.wpw.pim.service.cart;

import com.wpw.pim.domain.cart.CartItem;
import com.wpw.pim.domain.cart.CartItemId;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.cart.CartItemRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.cart.CartDto;
import com.wpw.pim.web.dto.cart.CartItemDto;
import com.wpw.pim.web.dto.cart.PriceTierDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final MediaFileRepository mediaFileRepository;
    private final DealerRepository dealerRepository;

    @Transactional(readOnly = true)
    public CartDto getCart(UUID dealerId) {
        Dealer dealer = loadDealer(dealerId);
        List<CartItem> items = cartItemRepository.findByDealerIdWithProduct(dealerId);

        UUID priceListId = dealer.getPriceList() != null ? dealer.getPriceList().getId() : null;
        String currency = (dealer.getPriceList() != null && dealer.getPriceList().getCurrency() != null)
            ? dealer.getPriceList().getCurrency().getCode()
            : "USD";

        List<UUID> productIds = items.stream().map(ci -> ci.getProduct().getId()).toList();

        // batch-load images (first image per product)
        Map<UUID, String> imageByProduct = productIds.isEmpty() ? Map.of()
            : mediaFileRepository.findByProductIds(productIds).stream()
                .filter(m -> m.getFileType() == FileType.image)
                .collect(Collectors.toMap(
                    m -> m.getProduct().getId(),
                    MediaFile::getUrl,
                    (existing, replacement) -> existing // keep first (lowest sort_order)
                ));

        // batch-load price tiers
        Map<UUID, List<PriceListItem>> tiersByProduct = Map.of();
        if (priceListId != null && !productIds.isEmpty()) {
            tiersByProduct = priceListItemRepository.findByPriceListIdAndProductIds(priceListId, productIds)
                .stream().collect(Collectors.groupingBy(i -> i.getProduct().getId()));
        }

        List<String> removedToolNos = new ArrayList<>();
        List<CartItemDto> dtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : items) {
            Product p = ci.getProduct();
            if (p.getStatus() != ProductStatus.active) {
                removedToolNos.add(p.getToolNo());
                continue;
            }

            List<PriceListItem> tierItems = tiersByProduct.getOrDefault(p.getId(), List.of());
            List<PriceTierDto> tiers = tierItems.stream()
                .map(t -> new PriceTierDto(t.getId().getMinQty(), t.getPrice()))
                .toList();

            BigDecimal unitPrice = tierItems.stream()
                .filter(t -> t.getId().getMinQty() <= ci.getQty())
                .max(Comparator.comparingInt(t -> t.getId().getMinQty()))
                .map(PriceListItem::getPrice)
                .orElse(null);

            BigDecimal lineTotal = unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(ci.getQty())) : null;
            if (lineTotal != null) total = total.add(lineTotal);

            String name = resolveProductName(p);
            String imageUrl = imageByProduct.get(p.getId());

            dtos.add(new CartItemDto(p.getId(), p.getToolNo(), name, imageUrl, ci.getQty(), unitPrice, lineTotal, tiers));
        }

        return new CartDto(dtos, currency, total, dtos.size(), removedToolNos);
    }

    @Transactional
    public CartDto addItems(UUID dealerId, List<UUID> productIds) {
        Dealer dealer = loadDealer(dealerId);
        for (UUID productId : productIds) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));
            CartItemId itemId = new CartItemId(dealerId, productId);
            CartItem existing = cartItemRepository.findById(itemId).orElse(null);
            if (existing != null) {
                existing.setQty(existing.getQty() + 1);
                cartItemRepository.save(existing);
            } else {
                cartItemRepository.save(new CartItem(dealer, product, 1));
            }
        }
        return getCart(dealerId);
    }

    @Transactional
    public CartDto addByFilter(UUID dealerId, List<UUID> allMatchingProductIds) {
        if (allMatchingProductIds.isEmpty()) return getCart(dealerId);
        Dealer dealer = loadDealer(dealerId);

        Set<UUID> alreadyInCart = Set.copyOf(cartItemRepository.findProductIdsByDealerId(dealerId));
        Map<UUID, Product> products = productRepository.findAllById(allMatchingProductIds)
            .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItem> toSave = new ArrayList<>();
        for (UUID productId : allMatchingProductIds) {
            if (alreadyInCart.contains(productId)) continue;
            Product p = products.get(productId);
            if (p != null && p.getStatus() == ProductStatus.active) {
                toSave.add(new CartItem(dealer, p, 1));
            }
        }
        if (!toSave.isEmpty()) cartItemRepository.saveAll(toSave);
        return getCart(dealerId);
    }

    @Transactional
    public CartDto updateQty(UUID dealerId, UUID productId, int qty) {
        if (qty <= 0) {
            cartItemRepository.deleteByDealerIdAndProductId(dealerId, productId);
            return getCart(dealerId);
        }
        CartItemId itemId = new CartItemId(dealerId, productId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart"));
        item.setQty(qty);
        cartItemRepository.save(item);
        return getCart(dealerId);
    }

    @Transactional
    public CartDto removeItem(UUID dealerId, UUID productId) {
        cartItemRepository.deleteByDealerIdAndProductId(dealerId, productId);
        return getCart(dealerId);
    }

    @Transactional
    public void clearCart(UUID dealerId) {
        cartItemRepository.deleteByDealerId(dealerId);
    }

    private Dealer loadDealer(UUID dealerId) {
        return dealerRepository.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found: " + dealerId));
    }

    private String resolveProductName(Product p) {
        if (p.getTranslations() == null || p.getTranslations().isEmpty()) return p.getToolNo();
        return p.getTranslations().stream()
            .filter(t -> "en".equals(t.getId().getLocale()))
            .findFirst()
            .or(() -> p.getTranslations().stream().findFirst())
            .map(t -> t.getName())
            .orElse(p.getToolNo());
    }
}
