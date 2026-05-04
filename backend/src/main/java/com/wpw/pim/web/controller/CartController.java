package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.cart.CartService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.cart.AddToCartRequest;
import com.wpw.pim.web.dto.cart.CartDto;
import com.wpw.pim.web.dto.cart.CartItemRequest;
import com.wpw.pim.web.dto.product.ProductFilter;
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
@RequestMapping("/api/v1/dealer/cart")
@PreAuthorize("hasRole('DEALER')")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Dealer cart: add items, manage, place order")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "DEALER role required")
})
public class CartController {

    private final CartService cartService;
    private final ProductService productService;
    private final DealerRepository dealerRepository;

    @GetMapping
    @Operation(summary = "Get cart")
    @ApiResponse(responseCode = "200", description = "Cart contents")
    public CartDto getCart(@AuthenticationPrincipal UserDetails principal) {
        return cartService.getCart(resolveDealerId(principal));
    }

    @PostMapping("/items")
    @Operation(summary = "Add items to cart")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    public CartDto addItems(
        @AuthenticationPrincipal UserDetails principal,
        @RequestBody AddToCartRequest request
    ) {
        return cartService.addItems(resolveDealerId(principal), request.items());
    }

    @PostMapping("/items/by-filter")
    @Operation(summary = "Add all items matching current filter")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    public CartDto addByFilter(
        @AuthenticationPrincipal UserDetails principal,
        @ModelAttribute ProductFilter filter
    ) {
        List<UUID> ids = productService.findAllIdsByFilter(filter);
        return cartService.addByFilter(resolveDealerId(principal), ids);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity in cart")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated cart"),
        @ApiResponse(responseCode = "404", description = "Product not in cart")
    })
    public CartDto updateQty(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID productId,
        @RequestParam int qty
    ) {
        return cartService.updateQty(resolveDealerId(principal), productId, qty);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart")
    @ApiResponse(responseCode = "200", description = "Updated cart")
    public CartDto removeItem(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID productId
    ) {
        return cartService.removeItem(resolveDealerId(principal), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared")
    public void clearCart(@AuthenticationPrincipal UserDetails principal) {
        cartService.clearCart(resolveDealerId(principal));
    }

    private UUID resolveDealerId(UserDetails principal) {
        if (principal instanceof DealerPrincipal dp) return dp.getDealer().getId();
        Dealer dealer = dealerRepository.findByUserUsername(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Dealer profile not found for: " + principal.getUsername()));
        return dealer.getId();
    }
}
