package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.web.dto.product.PriceInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceResolverService {

    private final PriceListRepository priceListRepo;
    private final PriceListItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final DealerRepository dealerRepo;

    @Transactional(readOnly = true)
    public PriceInfoDto resolve(String toolNo, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        Product product = productRepo.findByToolNo(toolNo).orElse(null);
        if (product == null) return null;

        // Admin path — show stock price
        if (hasAuthority(auth, "MANAGE_PRICES") || hasAuthority(auth, "MANAGE_CATALOG")) {
            return resolveStockPrice(product);
        }

        // Dealer path — show dealer price list
        if (principal instanceof DealerPrincipal dp) {
            return resolveDealerPrice(dp.getDealer(), product);
        }
        if (principal instanceof UserDetails ud) {
            if (hasRole(auth, "dealer")) {
                return dealerRepo.findByUserUsername(ud.getUsername())
                    .map(dealer -> resolveDealerPrice(dealer, product))
                    .orElse(null);
            }
        }

        return null;
    }

    private PriceInfoDto resolveStockPrice(Product product) {
        Optional<PriceList> stockOpt = priceListRepo.findFirstByType("stock");
        if (stockOpt.isEmpty()) return null;
        PriceList stock = stockOpt.get();

        List<PriceListItem> items = itemRepo.findByPriceListIdAndProductId(stock.getId(), product.getId());
        if (items.isEmpty()) return null;

        List<PriceInfoDto.TierDto> tiers = items.stream()
            .map(i -> new PriceInfoDto.TierDto(i.getId().getMinQty(), i.getPrice()))
            .toList();

        return new PriceInfoDto(
            stock.getCurrency().getCode(),
            stock.getCurrency().getSymbol(),
            tiers, false, null
        );
    }

    private PriceInfoDto resolveDealerPrice(Dealer dealer, Product product) {
        PriceList pl = dealer.getPriceList();
        if (pl == null) return null;

        List<PriceListItem> items = itemRepo.findByPriceListIdAndProductId(pl.getId(), product.getId());
        if (items.isEmpty()) return null;

        boolean expired = pl.getValidTo() != null && pl.getValidTo().isBefore(LocalDate.now());
        List<PriceInfoDto.TierDto> tiers = items.stream()
            .map(i -> new PriceInfoDto.TierDto(i.getId().getMinQty(), i.getPrice()))
            .toList();

        return new PriceInfoDto(
            pl.getCurrency().getCode(),
            pl.getCurrency().getSymbol(),
            tiers, expired, pl.getValidTo()
        );
    }

    // Batch resolve: one DB query per price list instead of one per product
    @Transactional(readOnly = true)
    public Map<UUID, PriceInfoDto> resolveBatch(List<UUID> productIds, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || productIds.isEmpty()) return Collections.emptyMap();

        Object principal = auth.getPrincipal();

        if (hasAuthority(auth, "MANAGE_PRICES") || hasAuthority(auth, "MANAGE_CATALOG")) {
            return priceListRepo.findFirstByType("stock")
                .map(stock -> buildPriceMap(stock, false, null, productIds))
                .orElse(Collections.emptyMap());
        }

        if (principal instanceof DealerPrincipal dp) {
            PriceList pl = dp.getDealer().getPriceList();
            if (pl == null) return Collections.emptyMap();
            boolean expired = pl.getValidTo() != null && pl.getValidTo().isBefore(LocalDate.now());
            return buildPriceMap(pl, expired, pl.getValidTo(), productIds);
        }

        if (principal instanceof UserDetails ud && hasRole(auth, "dealer")) {
            return dealerRepo.findByUserUsername(ud.getUsername())
                .map(dealer -> {
                    PriceList pl = dealer.getPriceList();
                    if (pl == null) return Collections.<UUID, PriceInfoDto>emptyMap();
                    boolean expired = pl.getValidTo() != null && pl.getValidTo().isBefore(LocalDate.now());
                    return buildPriceMap(pl, expired, pl.getValidTo(), productIds);
                })
                .orElse(Collections.emptyMap());
        }

        return Collections.emptyMap();
    }

    private Map<UUID, PriceInfoDto> buildPriceMap(PriceList pl, boolean expired,
                                                    LocalDate validTo, List<UUID> productIds) {
        List<PriceListItem> allItems = itemRepo.findByPriceListIdOrderByIdMinQtyAsc(pl.getId());
        String currCode = pl.getCurrency().getCode();
        String currSym = pl.getCurrency().getSymbol();

        return allItems.stream()
            .filter(i -> productIds.contains(i.getId().getProductId()))
            .collect(Collectors.groupingBy(
                i -> i.getId().getProductId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    items -> new PriceInfoDto(
                        currCode, currSym,
                        items.stream().map(i -> new PriceInfoDto.TierDto(i.getId().getMinQty(), i.getPrice())).toList(),
                        expired, validTo
                    )
                )
            ));
    }

    @Transactional(readOnly = true)
    public UUID resolvePriceListId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();

        if (hasAuthority(auth, "MANAGE_PRICES") || hasAuthority(auth, "MANAGE_CATALOG")) {
            return priceListRepo.findFirstByType("stock").map(PriceList::getId).orElse(null);
        }

        if (principal instanceof DealerPrincipal dp) {
            PriceList pl = dp.getDealer().getPriceList();
            return pl != null ? pl.getId() : null;
        }

        if (principal instanceof UserDetails ud && hasRole(auth, "dealer")) {
            return dealerRepo.findByUserUsername(ud.getUsername())
                .map(d -> d.getPriceList() != null ? d.getPriceList().getId() : null)
                .orElse(null);
        }

        return null;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equalsIgnoreCase("ROLE_" + role));
    }
}
