package com.wpw.pim.web.dto.dealer.admin;

public record DealerCreatedDto(
    DealerDto dealer,
    String username,
    String generatedPassword
) {}
