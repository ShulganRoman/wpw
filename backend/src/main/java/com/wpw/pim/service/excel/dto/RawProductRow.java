package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Строка листа Products в сыром виде — всё хранится как String.
 * Тип-конвертация и нормализация происходят в ImportValidator/ExcelImportService.
 */
@Getter
@Builder
public class RawProductRow {
    private final int    rowNum;
    private final String toolNo;
    private final String altToolNo;
    private final String categoryName;
    private final String groupId;
    private final String groupName;
    private final String description;
    private final String dMm;
    private final String d1Mm;
    private final String bMm;
    private final String b1Mm;
    private final String lMm;
    private final String l1Mm;
    private final String rMm;
    private final String aMm;
    private final String angleDeg;
    private final String shankMm;
    private final String shankInch;
    private final String flutes;
    private final String cuttingType;
    private final String ballBearing;
    private final String retainer;
    private final String bladeNo;
    private final String materials;
    private final String applications;
    private final String machines;
    private final String typeNote;
    private final String catalogPage;
}
