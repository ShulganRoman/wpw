package com.wpw.pim.web.dto.product;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;

import java.util.Set;

/**
 * DTO для обновления продукта.
 * <p>
 * Содержит все редактируемые поля продукта, включая переводы для конкретной locale
 * и коллекции материалов/типов. Поле toolNo намеренно отсутствует,
 * так как является неизменяемым идентификатором продукта.
 * </p>
 *
 * @param altToolNo           альтернативный артикул (опционально)
 * @param productType         тип продукта (main, spare_part, accessory)
 * @param status              статус продукта (active, discontinued, coming_soon)
 * @param isOrderable         доступен ли для заказа
 * @param catalogPage         страница каталога
 * @param name                название продукта (для перевода по указанной locale)
 * @param shortDescription    краткое описание
 * @param longDescription     полное описание
 * @param seoTitle            SEO-заголовок
 * @param seoDescription      SEO-описание
 * @param applications        области применения
 * @param attributes          технические характеристики продукта
 * @param toolMaterials       материалы инструмента
 * @param workpieceMaterials  материалы обрабатываемой заготовки
 * @param machineTypes        типы станков
 * @param machineBrands       бренды станков
 * @param operationCodes      коды операций
 */
public record ProductUpdateDto(
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
