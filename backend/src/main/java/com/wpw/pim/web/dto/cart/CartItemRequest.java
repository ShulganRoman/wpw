package com.wpw.pim.web.dto.cart;

import java.util.UUID;

public record CartItemRequest(UUID productId, int qty) {}
