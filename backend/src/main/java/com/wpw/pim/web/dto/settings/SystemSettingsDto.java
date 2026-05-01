package com.wpw.pim.web.dto.settings;

public record SystemSettingsDto(
    boolean requireImagesAdmin,
    boolean requireImagesDealer,
    boolean requireImagesPublic,
    boolean requirePriceAdmin,
    boolean requirePriceDealer,
    boolean requirePricePublic
) {}
