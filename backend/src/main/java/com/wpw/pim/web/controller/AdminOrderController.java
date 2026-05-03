package com.wpw.pim.web.controller;

import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.ChangeStatusRequest;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_DEALERS')")
@Tag(name = "Admin: Orders", description = "Dealer order management")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "MANAGE_DEALERS required")
})
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/api/v1/admin/dealers/{dealerId}/orders")
    @Operation(summary = "List dealer orders")
    @ApiResponse(responseCode = "200", description = "Dealer order list")
    public List<OrderSummaryDto> getDealerOrders(@PathVariable UUID dealerId) {
        return orderService.getAdminDealerOrders(dealerId);
    }

    @GetMapping("/api/v1/admin/orders/{orderId}")
    @Operation(summary = "Order details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order details"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDto getOrder(@PathVariable UUID orderId) {
        return orderService.getAdminOrder(orderId);
    }

    @PatchMapping("/api/v1/admin/orders/{orderId}/status")
    @Operation(summary = "Change order status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status changed"),
        @ApiResponse(responseCode = "400", description = "Invalid status"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public OrderDto changeStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody ChangeStatusRequest req
    ) {
        return orderService.changeStatus(orderId, req.status());
    }

    @GetMapping("/api/v1/admin/dealers/{dealerId}/orders/pending")
    @Operation(summary = "Check if dealer has open orders")
    @ApiResponse(responseCode = "200", description = "Result returned")
    public boolean hasPending(@PathVariable UUID dealerId) {
        return orderService.hasPendingOrders(dealerId);
    }

    @GetMapping("/api/v1/admin/orders/pending-dealer-ids")
    @Operation(summary = "IDs of dealers with open orders")
    @ApiResponse(responseCode = "200", description = "Result returned")
    public java.util.Set<UUID> pendingDealerIds() {
        return orderService.getDealerIdsWithPendingOrders();
    }
}
