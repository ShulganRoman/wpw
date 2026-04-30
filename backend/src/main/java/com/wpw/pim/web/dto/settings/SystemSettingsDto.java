package com.wpw.pim.web.dto.settings;

public record SystemSettingsDto(
    boolean requireImagesAdmin,
    boolean requireImagesDealer,
    boolean requireImagesPublic
) {}
