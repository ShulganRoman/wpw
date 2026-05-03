package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.CheckoutResponse;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "DEALER role required")
})
public class DealerOrderController {

    private final OrderService orderService;
    private final DealerRepository dealerRepository;

    @PostMapping("/cart/checkout")
    @Operation(summary = "Place order from cart")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order placed"),
        @ApiResponse(responseCode = "400", description = "Cart is empty")
    })
    public CheckoutResponse checkout(@AuthenticationPrincipal UserDetails principal) {
        return orderService.checkout(resolveDealerId(principal));
    }

    @GetMapping("/orders")
    @Operation(summary = "Dealer order history")
    @ApiResponse(responseCode = "200", description = "Order list")
    public List<OrderSummaryDto> myOrders(@AuthenticationPrincipal UserDetails principal) {
        return orderService.getDealerOrders(resolveDealerId(principal));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Order details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order details"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
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
