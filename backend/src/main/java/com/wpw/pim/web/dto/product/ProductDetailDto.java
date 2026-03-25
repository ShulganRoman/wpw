package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ProductDetailDto(
    UUID id,
    String toolNo,
    String altToolNo,
    ProductType productType,
    ProductStatus status,
    boolean isOrderable,
    Short catalogPage,
    String name,
    String shortDescription,
    String longDescription,
    String seoTitle,
    String seoDescription,
    String applications,
    boolean aiGenerated,
    String locale,
    boolean isRtl,
    ProductAttributesDto attributes,
    Set<String> toolMaterials,
    Set<String> workpieceMaterials,
    Set<String> machineTypes,
    Set<String> machineBrands,
    Set<String> operationCodes,
    List<String> mediaUrls,
    String thumbnailUrl,
    String jsonLd,
    String groupName,
    String categoryName,
    String sectionName
) {}
