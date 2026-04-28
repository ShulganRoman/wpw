package com.wpw.pim.web.dto.dealer.admin;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DealerSaveRequest(
    @NotBlank String dealerCode,
    @NotBlank String companyName,
    @NotBlank String country,
    String brandName,
    String dealerType,
    String privateLabelBrand,
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
    List<DealerContactSaveRequest> contacts
) {}
