package com.wpw.pim.web.dto.dealer.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DealerDto(
    UUID id,
    String dealerCode,
    String companyName,
    String brandName,
    String dealerType,
    String privateLabelBrand,
    String country,
    String region,
    String city,
    String address,
    String postalCode,
    String website,
    boolean hasEcommerce,
    String shopUrl,
    String logo,
    String priceListId,
    String currency,
    String discountTier,
    String notes,
    boolean isActive,
    String username,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<DealerContactDto> contacts
) {}
