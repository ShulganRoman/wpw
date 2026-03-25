package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Одна проблема, обнаруженная при валидации.
 */
@Getter
@Builder
public class ValidationIssue {

    public enum Severity { ERROR, WARNING }

    public enum Sheet { PRODUCTS, GROUPS }

    private final Severity severity;
    private final Sheet    sheet;
    private final int      rowNum;
    private final String   field;
    private final String   rawValue;
    private final String   message;

    public static ValidationIssue error(Sheet sheet, int row, String field, String value, String msg) {
        return ValidationIssue.builder()
            .severity(Severity.ERROR).sheet(sheet).rowNum(row)
            .field(field).rawValue(value).message(msg).build();
    }

    public static ValidationIssue warning(Sheet sheet, int row, String field, String value, String msg) {
        return ValidationIssue.builder()
            .severity(Severity.WARNING).sheet(sheet).rowNum(row)
            .field(field).rawValue(value).message(msg).build();
    }
}
