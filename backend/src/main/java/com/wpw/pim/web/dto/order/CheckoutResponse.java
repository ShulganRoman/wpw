package com.wpw.pim.web.dto.order;

import java.util.UUID;

public record CheckoutResponse(UUID orderId, String message) {}
