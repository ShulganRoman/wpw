package com.wpw.pim.service.excel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * Конфигурация импорта из Excel.
 * Названия листов и заголовков колонок задаются в application.properties под префиксом pim.import.
 * Если структура Excel изменится — достаточно обновить конфиг, не трогая логику.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pim.import")
public class ExcelImportProperties {

    /** Строка заголовков (1-based). */
    private int headerRow = 2;

    /** Строка начала данных (1-based). */
    private int dataStartRow = 3;

    @NestedConfigurationProperty
    private ProductsSheet productsSheet = new ProductsSheet();

    @NestedConfigurationProperty
    private GroupsSheet groupsSheet = new GroupsSheet();

    @Getter
    @Setter
    public static class ProductsSheet {
        private String name = "Products";

        /** Названия колонок в Excel — ключи = поля Java, значения = заголовки Excel. */
        private String toolNo       = "Tool No";
        private String altToolNo    = "Alt Tool No";
        private String category     = "Category";
        private String groupId      = "Group ID";
        private String groupName    = "Group Name";
        private String description  = "Description";
        private String dMm          = "D (mm)";
        private String d1Mm         = "D1 (mm)";
        private String bMm          = "B / Cut. Length (mm)";
        private String b1Mm         = "B1 (mm)";
        private String lMm          = "L / Total (mm)";
        private String l1Mm         = "L1 (mm)";
        private String rMm          = "R (mm)";
        private String aMm          = "A (mm)";
        private String angleDeg     = "Angle (°)";
        private String shankMm      = "Shank (mm)";
        private String shankInch    = "Shank (in)";
        private String flutes       = "Flutes";
        private String cuttingType  = "Cutting Type";
        private String ballBearing  = "Ball Bearing";
        private String retainer     = "Retainer";
        private String bladeNo      = "Blade No";
        private String materials    = "Materials";
        private String applications = "Applications";
        private String machines     = "Machines";
        private String typeNote     = "Type / Note";
        private String catalogPage  = "Catalog Page";

        // --- New fields covering full UI surface ---
        private String productName        = "Name";
        private String shortDescription   = "Short Description";
        private String longDescription    = "Long Description";
        private String status             = "Status";
        private String orderable          = "Orderable";
        private String productType        = "Product Type";
        private String d2Mm               = "D2 (mm)";
        private String rotationDirection  = "Rotation Direction";
        private String boreType           = "Bore Type";
        private String hasBallBearing     = "Has Ball Bearing";
        private String hasRetainer        = "Has Retainer";
        private String canResharpen       = "Can Resharpen";
        private String toolMaterials      = "Tool Materials";
        private String workpieceMaterials = "Workpiece Materials";
        private String machineTypes       = "Machine Types";
        private String machineBrands      = "Machine Brands";
        private String applicationTags    = "Application Tags";
        private String ean13              = "EAN-13";
        private String upc12              = "UPC-12";
        private String hsCode             = "HS Code";
        private String countryOfOrigin    = "Country of Origin";
        private String weightG            = "Weight (g)";
        private String pkgQty             = "Package Qty";
        private String cartonQty          = "Carton Qty";
        private String stockStatus        = "Stock Status";
        private String stockQty           = "Stock Qty";
    }

    @Getter
    @Setter
    public static class GroupsSheet {
        private String name = "Product Groups";

        private String groupId           = "Group ID";
        private String groupCode         = "Group Code";
        private String groupName         = "Group Name";
        private String category          = "Category";
        private String shortDescription  = "Short Description";
        private String applications      = "Typical Applications";
        private String materials         = "Materials";
        private String machines          = "Machines / Equipment";
        private String catalogPages      = "Catalog Pages";
    }
}
