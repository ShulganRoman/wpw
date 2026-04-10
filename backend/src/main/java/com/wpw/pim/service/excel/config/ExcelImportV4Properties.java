package com.wpw.pim.service.excel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * Конфигурация импорта формата v4 (один лист "Products", без Group ID).
 * <p>
 * Группы определяются автоматически по паре Category + Group Name.
 * Все заголовки настраиваются через application.properties (pim.import.v4.*).
 * </p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pim.import.v4")
public class ExcelImportV4Properties {

    /** Строка заголовков (1-based). */
    private int headerRow = 2;

    /** Строка начала данных (1-based). */
    private int dataStartRow = 3;

    /** Название листа с продуктами. */
    private String sheetName = "Products";

    @NestedConfigurationProperty
    private Columns columns = new Columns();

    /**
     * Названия колонок в Excel-шаблоне v4.
     * Порядок соответствует реальному шаблону пользователя.
     */
    @Getter
    @Setter
    public static class Columns {
        private String toolNo             = "Tool No";
        private String altToolNo          = "Alt Tool No";
        private String name               = "Name";
        private String shortDescription   = "Short Description";
        private String longDescription    = "Long Description";
        private String productType        = "Product Type";
        private String category           = "Category";
        private String groupName          = "Group Name";
        private String status             = "Status";
        private String orderable          = "Orderable";
        private String catalogPage        = "Catalog Page";
        private String dMm               = "D (mm)";
        private String d1Mm              = "D1 (mm)";
        private String d2Mm              = "D2 (mm)";
        private String bMm              = "B / Cut. Length (mm)";
        private String b1Mm              = "B1 (mm)";
        private String lMm              = "L / Total (mm)";
        private String l1Mm              = "L1 (mm)";
        private String rMm              = "R (mm)";
        private String aMm              = "A (mm)";
        private String angleDeg          = "Angle (\u00b0)";
        private String shankMm          = "Shank (mm)";
        private String shankInch        = "Shank (in)";
        private String flutes            = "Flutes";
        private String bladeNo           = "Blade No";
        private String cuttingType       = "Cutting Type";
        private String rotationDirection = "Rotation Direction";
        private String boreType          = "Bore Type";
        private String ballBearing       = "Ball Bearing";
        private String hasBallBearing    = "Has Ball Bearing";
        private String retainer          = "Retainer";
        private String hasRetainer       = "Has Retainer";
        private String canResharpen      = "Can Resharpen";
        private String toolMaterials     = "Tool Materials";
        private String workpieceMaterials = "Workpiece Materials";
        private String machineTypes      = "Machine Types";
        private String machineBrands     = "Machine Brands";
        private String applicationTags   = "Application Tags";
        private String ean13             = "EAN-13";
        private String upc12             = "UPC-12";
        private String hsCode            = "HS Code";
        private String countryOfOrigin   = "Country of Origin";
        private String weightG           = "Weight (g)";
        private String pkgQty            = "Package Qty";
        private String cartonQty         = "Carton Qty";
        private String stockStatus       = "Stock Status";
        private String stockQty          = "Stock Qty";
        private String typeNote          = "Type / Note";
    }
}
