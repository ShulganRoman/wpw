package com.wpw.pim.web.dto.cart;

import java.util.List;

public record AddToCartRequest(List<CartItemRequest> items) {}
