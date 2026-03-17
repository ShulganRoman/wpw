package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.List;

/**
 * Статистика выполненного импорта — используется для генерации MD-отчёта.
 */
@Getter
@Builder
public class ImportStats {
    private final String    importedAt;
    private final Duration  duration;

    // Каталог
    private final int sectionsCreated;
    private final int categoriesCreated;
    private final int categoriesFound;
    private final int groupsCreated;
    private final int groupsFound;

    // Товары
    private final int totalProductRows;
    private final int productsCreated;
    private final int productsUpdated;
    private final int productsSkipped;   // пустой toolNo или fatal error по строке

    // Проблемы при выполнении
    private final List<String> executionErrors;
    private final List<String> executionWarnings;
}
