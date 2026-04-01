package com.wpw.pim.web.dto.media;

import java.util.UUID;

/**
 * DTO для представления изображения товара.
 *
 * @param id        идентификатор медиафайла
 * @param url       URL изображения
 * @param sortOrder порядок сортировки
 */
public record MediaImageDto(UUID id, String url, int sortOrder) {}
