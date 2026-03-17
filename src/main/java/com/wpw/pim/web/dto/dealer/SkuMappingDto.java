package com.wpw.pim.web.dto.dealer;

import java.util.UUID;

public record SkuMappingDto(UUID productId, String toolNo, String dealerSku) {}
