package com.wpw.pim.web.dto.pricing;

import java.time.LocalDate;
import java.util.List;

public record DealerPriceListDto(
    String currencyCode,
    String currencySymbol,
    LocalDate validFrom,
    LocalDate validTo,
    boolean expired,
    List<PriceListItemDto> items
) {}
