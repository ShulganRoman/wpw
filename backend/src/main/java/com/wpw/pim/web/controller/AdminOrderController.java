package com.wpw.pim.web.controller;

import com.wpw.pim.service.order.OrderService;
import com.wpw.pim.web.dto.order.ChangeStatusRequest;
import com.wpw.pim.web.dto.order.OrderDto;
import com.wpw.pim.web.dto.order.OrderSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
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
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/api/v1/admin/dealers/{dealerId}/orders")
    @Operation(summary = "List dealer orders")
    public List<OrderSummaryDto> getDealerOrders(@PathVariable UUID dealerId) {
        return orderService.getAdminDealerOrders(dealerId);
    }

    @GetMapping("/api/v1/admin/orders/{orderId}")
    @Operation(summary = "Order details")
    public OrderDto getOrder(@PathVariable UUID orderId) {
        return orderService.getAdminOrder(orderId);
    }

    @PatchMapping("/api/v1/admin/orders/{orderId}/status")
    @Operation(summary = "Change order status")
    public OrderDto changeStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody ChangeStatusRequest req
    ) {
        return orderService.changeStatus(orderId, req.status());
    }

    @GetMapping("/api/v1/admin/dealers/{dealerId}/orders/pending")
    @Operation(summary = "Check if dealer has open orders")
    public boolean hasPending(@PathVariable UUID dealerId) {
        return orderService.hasPendingOrders(dealerId);
    }

    @GetMapping("/api/v1/admin/orders/pending-dealer-ids")
    @Operation(summary = "IDs of dealers with open orders")
    public java.util.Set<UUID> pendingDealerIds() {
        return orderService.getDealerIdsWithPendingOrders();
    }
}
