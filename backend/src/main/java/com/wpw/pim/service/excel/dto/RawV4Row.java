package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Строка листа Products формата v4 в сыром виде.
 * <p>
 * Все поля хранятся как String (кроме rowNum).
 * Тип-конвертация происходит в {@code ExcelImportV4Service}.
 * Группа определяется по паре (categoryName, groupName) — явный groupId не используется.
 * </p>
 */
@Getter
@Builder
public class RawV4Row {
    private final int    rowNum;
    private final String toolNo;
    private final String altToolNo;
    private final String name;
    private final String shortDescription;
    private final String longDescription;
    private final String productType;
    private final String categoryName;
    private final String groupName;
    private final String status;
    private final String orderable;
    private final String catalogPage;
    private final String dMm;
    private final String d1Mm;
    private final String d2Mm;
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
    private final String bladeNo;
    private final String cuttingType;
    private final String rotationDirection;
    private final String boreType;
    private final String ballBearing;
    private final String hasBallBearing;
    private final String retainer;
    private final String hasRetainer;
    private final String canResharpen;
    private final String toolMaterials;
    private final String workpieceMaterials;
    private final String machineTypes;
    private final String machineBrands;
    private final String applicationTags;
    private final String ean13;
    private final String upc12;
    private final String hsCode;
    private final String countryOfOrigin;
    private final String weightG;
    private final String pkgQty;
    private final String cartonQty;
    private final String stockStatus;
    private final String stockQty;
    private final String typeNote;
}
