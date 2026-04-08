package com.wpw.pim.service.excel.parser;

import com.wpw.pim.service.excel.dto.WpwCatalogRow;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Парсер формата WPW_Catalog_v3.xlsx (один лист, заголовки в строке 1).
 * SEO-колонки (SEO_Title_EN, SEO_Description_EN, Keywords_EN, URL_Slug) игнорируются.
 */
@Component
public class WpwCatalogParser {

    public static final String SHEET_NAME = "Sheet1";

    private static final Set<String> KNOWN_HEADERS = Set.of(
        "SKU", "Product_Type", "Category", "Group",
        "Description_EN", "Name_RU", "AI_Description_EN",
        "D_mm", "D1_mm", "B_mm", "L_mm", "R_mm", "Angle_deg",
        "Shank_mm", "Shank_inch", "Flutes",
        "Tool_Material", "Cutting_Type", "Application_Tags",
        "Workpiece_Material", "Machine_Type",
        "Data_Completeness", "Spare_Parts", "Related_Products", "Set_Components",
        // SEO columns — known but intentionally skipped
        "SEO_Title_EN", "SEO_Description_EN", "Keywords_EN", "URL_Slug"
    );

    public List<WpwCatalogRow> parse(Sheet sheet, FormulaEvaluator evaluator) {
        Row headerRow = sheet.getRow(0); // строка 1 (0-based)
        ColumnIndex idx = new ColumnIndex(headerRow);

        List<WpwCatalogRow> result = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String sku = r(row, idx.get("SKU"), evaluator);
            if (sku == null || sku.isBlank()) continue;

            result.add(WpwCatalogRow.builder()
                .rowNum(i + 1)
                .sku(sku)
                .productType(r(row, idx.get("Product_Type"), evaluator))
                .category(r(row, idx.get("Category"), evaluator))
                .group(r(row, idx.get("Group"), evaluator))
                .descriptionEn(r(row, idx.get("Description_EN"), evaluator))
                .nameRu(r(row, idx.get("Name_RU"), evaluator))
                .aiDescriptionEn(r(row, idx.get("AI_Description_EN"), evaluator))
                .dMm(r(row, idx.get("D_mm"), evaluator))
                .d1Mm(r(row, idx.get("D1_mm"), evaluator))
                .bMm(r(row, idx.get("B_mm"), evaluator))
                .lMm(r(row, idx.get("L_mm"), evaluator))
                .rMm(r(row, idx.get("R_mm"), evaluator))
                .angleDeg(r(row, idx.get("Angle_deg"), evaluator))
                .shankMm(r(row, idx.get("Shank_mm"), evaluator))
                .shankInch(r(row, idx.get("Shank_inch"), evaluator))
                .flutes(r(row, idx.get("Flutes"), evaluator))
                .toolMaterial(r(row, idx.get("Tool_Material"), evaluator))
                .cuttingType(r(row, idx.get("Cutting_Type"), evaluator))
                .applicationTags(r(row, idx.get("Application_Tags"), evaluator))
                .workpieceMaterial(r(row, idx.get("Workpiece_Material"), evaluator))
                .machineType(r(row, idx.get("Machine_Type"), evaluator))
                .dataCompleteness(r(row, idx.get("Data_Completeness"), evaluator))
                .spareParts(r(row, idx.get("Spare_Parts"), evaluator))
                .relatedProducts(r(row, idx.get("Related_Products"), evaluator))
                .setComponents(r(row, idx.get("Set_Components"), evaluator))
                .build());
        }
        return result;
    }

    /** Заголовки из файла, не относящиеся к формату WPW Catalog v3. */
    public List<String> unknownHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        ColumnIndex idx = new ColumnIndex(headerRow);
        return idx.foundHeaders().stream()
            .filter(h -> !KNOWN_HEADERS.contains(h))
            .sorted()
            .toList();
    }

    private static String r(Row row, int col, FormulaEvaluator ev) {
        return CellReader.read(row, col, ev);
    }
}
