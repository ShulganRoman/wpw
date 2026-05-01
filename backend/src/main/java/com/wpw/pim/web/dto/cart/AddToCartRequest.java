package com.wpw.pim.web.dto.cart;

import java.util.List;
import java.util.UUID;

public record AddToCartRequest(List<UUID> productIds) {}
