package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.dealer.DealerService;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dealer")
@PreAuthorize("hasRole('DEALER')")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;

    @GetMapping("/sku-mapping")
    public List<SkuMappingDto> getSkuMapping(@AuthenticationPrincipal DealerPrincipal principal) {
        return dealerService.getSkuMapping(principal.getDealer().getId());
    }

    @PostMapping("/sku-mapping")
    public SkuMappingDto addSkuMapping(
        @AuthenticationPrincipal DealerPrincipal principal,
        @Valid @RequestBody SkuMappingCreateRequest request
    ) {
        Dealer dealer = principal.getDealer();
        return dealerService.saveSkuMapping(dealer.getId(), request, dealer);
    }

    @GetMapping("/price-list")
    public PriceListDto getPriceList(@AuthenticationPrincipal DealerPrincipal principal) {
        return dealerService.getPriceList(principal.getDealer());
    }
}
