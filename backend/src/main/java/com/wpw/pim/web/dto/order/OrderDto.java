package com.wpw.pim.web.dto.order;

import com.wpw.pim.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDto(
    UUID id,
    UUID dealerId,
    String dealerName,
    OrderStatus status,
    String statusLabel,
    String currency,
    BigDecimal total,
    OffsetDateTime submittedAt,
    OffsetDateTime updatedAt,
    List<OrderItemDto> items,
    String comment
) {}
