package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.CheckoutResponse;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dealer")
@PreAuthorize("hasRole('DEALER')")
@RequiredArgsConstructor
@Tag(name = "Dealer: Orders", description = "Dealer orders")
public class DealerOrderController {

    private final OrderService orderService;
    private final DealerRepository dealerRepository;

    @PostMapping("/cart/checkout")
    @Operation(summary = "Place order from cart")
    public CheckoutResponse checkout(@AuthenticationPrincipal UserDetails principal) {
        return orderService.checkout(resolveDealerId(principal));
    }

    @GetMapping("/orders")
    @Operation(summary = "Dealer order history")
    public List<OrderSummaryDto> myOrders(@AuthenticationPrincipal UserDetails principal) {
        return orderService.getDealerOrders(resolveDealerId(principal));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Order details")
    public OrderDto myOrder(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID orderId
    ) {
        return orderService.getDealerOrder(resolveDealerId(principal), orderId);
    }

    private UUID resolveDealerId(UserDetails principal) {
        if (principal instanceof DealerPrincipal dp) return dp.getDealer().getId();
        Dealer dealer = dealerRepository.findByUserUsername(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Dealer profile not found for: " + principal.getUsername()));
        return dealer.getId();
    }
}
