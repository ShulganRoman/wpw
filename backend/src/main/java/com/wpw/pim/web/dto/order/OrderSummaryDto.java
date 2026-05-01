package com.wpw.pim.web.dto.order;

import com.wpw.pim.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSummaryDto(
    UUID id,
    OrderStatus status,
    String statusLabel,
    String currency,
    BigDecimal total,
    int itemCount,
    OffsetDateTime submittedAt,
    OffsetDateTime updatedAt
) {}
