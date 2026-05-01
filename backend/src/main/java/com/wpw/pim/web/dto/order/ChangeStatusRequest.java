package com.wpw.pim.web.dto.order;

import com.wpw.pim.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {}
