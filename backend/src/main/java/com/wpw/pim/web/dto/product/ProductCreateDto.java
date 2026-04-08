package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;

import java.util.Set;
import java.util.UUID;

/**
 * DTO для создания нового продукта со всеми атрибутами.
 *
 * @param toolNo             артикул (обязательный, уникальный)
 * @param groupId            идентификатор группы продуктов (опционально)
 * @param altToolNo          альтернативный артикул
 * @param productType        тип продукта (по умолчанию main)
 * @param status             статус (по умолчанию active)
 * @param isOrderable        доступен ли для заказа
 * @param catalogPage        страница каталога
 * @param name               название продукта на английском
 * @param shortDescription   краткое описание
 * @param longDescription    полное описание
 * @param seoTitle           SEO-заголовок
 * @param seoDescription     SEO-описание
 * @param applications       области применения
 * @param attributes         технические характеристики
 * @param toolMaterials      материалы инструмента
 * @param workpieceMaterials материалы заготовки
 * @param machineTypes       типы станков
 * @param machineBrands      бренды станков
 * @param operationCodes     коды операций
 */
public record ProductCreateDto(
    String toolNo,
    UUID groupId,
    String altToolNo,
    ProductType productType,
    ProductStatus status,
    Boolean isOrderable,
    Short catalogPage,
    String name,
    String shortDescription,
    String longDescription,
    String seoTitle,
    String seoDescription,
    String applications,
    ProductAttributesDto attributes,
    Set<String> toolMaterials,
    Set<String> workpieceMaterials,
    Set<String> machineTypes,
    Set<String> machineBrands,
    Set<String> operationCodes
) {}
