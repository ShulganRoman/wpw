package com.wpw.pim.service.excel.parser;

import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.RawProductRow;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсер листа Products.
 * Строит индекс колонок по заголовкам из конфига — порядок колонок в Excel не важен.
 */
@Component
@RequiredArgsConstructor
public class ProductsSheetParser {

    private final ExcelImportProperties props;

    public List<RawProductRow> parse(Sheet sheet, FormulaEvaluator evaluator) {
        ExcelImportProperties.ProductsSheet cfg = props.getProductsSheet();
        int headerRowIdx = props.getHeaderRow() - 1; // 0-based
        int dataStartIdx = props.getDataStartRow() - 1;

        Row headerRow = sheet.getRow(headerRowIdx);
        ColumnIndex idx = new ColumnIndex(headerRow);

        List<RawProductRow> result = new ArrayList<>();
        for (int i = dataStartIdx; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Пропускаем полностью пустые строки
            String toolNo = r(row, idx.get(cfg.getToolNo()), evaluator);
            String groupId = r(row, idx.get(cfg.getGroupId()), evaluator);
            if (toolNo == null && groupId == null) continue;

            result.add(RawProductRow.builder()
                .rowNum(i + 1) // 1-based для отображения
                .toolNo(toolNo)
                .altToolNo(r(row, idx.get(cfg.getAltToolNo()), evaluator))
                .categoryName(r(row, idx.get(cfg.getCategory()), evaluator))
                .groupId(groupId)
                .groupName(r(row, idx.get(cfg.getGroupName()), evaluator))
                .description(r(row, idx.get(cfg.getDescription()), evaluator))
                .dMm(r(row, idx.get(cfg.getDMm()), evaluator))
                .d1Mm(r(row, idx.get(cfg.getD1Mm()), evaluator))
                .bMm(r(row, idx.get(cfg.getBMm()), evaluator))
                .b1Mm(r(row, idx.get(cfg.getB1Mm()), evaluator))
                .lMm(r(row, idx.get(cfg.getLMm()), evaluator))
                .l1Mm(r(row, idx.get(cfg.getL1Mm()), evaluator))
                .rMm(r(row, idx.get(cfg.getRMm()), evaluator))
                .aMm(r(row, idx.get(cfg.getAMm()), evaluator))
                .angleDeg(r(row, idx.get(cfg.getAngleDeg()), evaluator))
                .shankMm(r(row, idx.get(cfg.getShankMm()), evaluator))
                .shankInch(r(row, idx.get(cfg.getShankInch()), evaluator))
                .flutes(r(row, idx.get(cfg.getFlutes()), evaluator))
                .cuttingType(r(row, idx.get(cfg.getCuttingType()), evaluator))
                .ballBearing(r(row, idx.get(cfg.getBallBearing()), evaluator))
                .retainer(r(row, idx.get(cfg.getRetainer()), evaluator))
                .bladeNo(r(row, idx.get(cfg.getBladeNo()), evaluator))
                .materials(r(row, idx.get(cfg.getMaterials()), evaluator))
                .applications(r(row, idx.get(cfg.getApplications()), evaluator))
                .machines(r(row, idx.get(cfg.getMachines()), evaluator))
                .typeNote(r(row, idx.get(cfg.getTypeNote()), evaluator))
                .catalogPage(r(row, idx.get(cfg.getCatalogPage()), evaluator))
                .build());
        }
        return result;
    }

    private static String r(Row row, int col, FormulaEvaluator ev) {
        return CellReader.read(row, col, ev);
    }

    /** Возвращает список заголовков из файла, не совпадающих ни с одним из конфига. */
    public List<String> unknownHeaders(Sheet sheet) {
        // evaluator не нужен для чтения заголовков
        ExcelImportProperties.ProductsSheet cfg = props.getProductsSheet();
        Row headerRow = sheet.getRow(props.getHeaderRow() - 1);
        ColumnIndex idx = new ColumnIndex(headerRow);

        java.util.Set<String> configured = java.util.Set.of(
            cfg.getToolNo(), cfg.getAltToolNo(), cfg.getCategory(), cfg.getGroupId(),
            cfg.getGroupName(), cfg.getDescription(), cfg.getDMm(), cfg.getD1Mm(),
            cfg.getBMm(), cfg.getB1Mm(), cfg.getLMm(), cfg.getL1Mm(), cfg.getRMm(),
            cfg.getAMm(), cfg.getAngleDeg(), cfg.getShankMm(), cfg.getShankInch(),
            cfg.getFlutes(), cfg.getCuttingType(), cfg.getBallBearing(), cfg.getRetainer(),
            cfg.getBladeNo(), cfg.getMaterials(), cfg.getApplications(), cfg.getMachines(),
            cfg.getTypeNote(), cfg.getCatalogPage()
        );

        return idx.foundHeaders().stream()
            .filter(h -> !configured.contains(h))
            .sorted()
            .toList();
    }
}
