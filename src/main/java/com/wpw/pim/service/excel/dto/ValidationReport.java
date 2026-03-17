package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Результат предимпортной валидации — возвращается администратору для ревью.
 */
@Getter
@Builder
public class ValidationReport {

    private final int totalProductRows;
    private final int totalGroupRows;
    private final int errorCount;
    private final int warningCount;

    /** Можно ли продолжать импорт (нет ERRORs). */
    private final boolean canProceed;

    /** Все ошибки и предупреждения с полным контекстом. */
    private final List<ValidationIssue> issues;

    /** Заголовки из файла, которые не были распознаны (помогают обнаружить переименование колонок). */
    private final List<String> unknownHeaders;
}
