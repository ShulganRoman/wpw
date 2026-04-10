package com.wpw.pim.service.excel.parser;

import com.wpw.pim.service.excel.config.ExcelImportV4Properties;
import com.wpw.pim.service.excel.dto.RawV4Row;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Парсер листа Products формата v4.
 * <p>
 * Строит индекс колонок по заголовкам из {@link ExcelImportV4Properties} —
 * порядок колонок в Excel не важен, важны только названия.
 * Пропускает строки, где Tool No пуст (null / blank).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class V4SheetParser {

    private final ExcelImportV4Properties props;

    /**
     * Парсит лист Products и возвращает список сырых строк.
     *
     * @param sheet     лист Excel
     * @param evaluator вычислитель формул (может быть null)
     * @return список строк с данными (строки без toolNo пропускаются)
     */
    public List<RawV4Row> parse(Sheet sheet, FormulaEvaluator evaluator) {
        ExcelImportV4Properties.Columns cfg = props.getColumns();
        int headerRowIdx = props.getHeaderRow() - 1;
        int dataStartIdx = props.getDataStartRow() - 1;

        Row headerRow = sheet.getRow(headerRowIdx);
        ColumnIndex idx = new ColumnIndex(headerRow);

        List<RawV4Row> result = new ArrayList<>();
        for (int i = dataStartIdx; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String toolNo = r(row, idx.get(cfg.getToolNo()), evaluator);
            if (toolNo == null) continue;

            result.add(RawV4Row.builder()
                .rowNum(i + 1)
                .toolNo(toolNo)
                .altToolNo(r(row, idx.get(cfg.getAltToolNo()), evaluator))
                .name(r(row, idx.get(cfg.getName()), evaluator))
                .shortDescription(r(row, idx.get(cfg.getShortDescription()), evaluator))
                .longDescription(r(row, idx.get(cfg.getLongDescription()), evaluator))
                .productType(r(row, idx.get(cfg.getProductType()), evaluator))
                .categoryName(r(row, idx.get(cfg.getCategory()), evaluator))
                .groupName(r(row, idx.get(cfg.getGroupName()), evaluator))
                .status(r(row, idx.get(cfg.getStatus()), evaluator))
                .orderable(r(row, idx.get(cfg.getOrderable()), evaluator))
                .catalogPage(r(row, idx.get(cfg.getCatalogPage()), evaluator))
                .dMm(r(row, idx.get(cfg.getDMm()), evaluator))
                .d1Mm(r(row, idx.get(cfg.getD1Mm()), evaluator))
                .d2Mm(r(row, idx.get(cfg.getD2Mm()), evaluator))
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
                .bladeNo(r(row, idx.get(cfg.getBladeNo()), evaluator))
                .cuttingType(r(row, idx.get(cfg.getCuttingType()), evaluator))
                .rotationDirection(r(row, idx.get(cfg.getRotationDirection()), evaluator))
                .boreType(r(row, idx.get(cfg.getBoreType()), evaluator))
                .ballBearing(r(row, idx.get(cfg.getBallBearing()), evaluator))
                .hasBallBearing(r(row, idx.get(cfg.getHasBallBearing()), evaluator))
                .retainer(r(row, idx.get(cfg.getRetainer()), evaluator))
                .hasRetainer(r(row, idx.get(cfg.getHasRetainer()), evaluator))
                .canResharpen(r(row, idx.get(cfg.getCanResharpen()), evaluator))
                .toolMaterials(r(row, idx.get(cfg.getToolMaterials()), evaluator))
                .workpieceMaterials(r(row, idx.get(cfg.getWorkpieceMaterials()), evaluator))
                .machineTypes(r(row, idx.get(cfg.getMachineTypes()), evaluator))
                .machineBrands(r(row, idx.get(cfg.getMachineBrands()), evaluator))
                .applicationTags(r(row, idx.get(cfg.getApplicationTags()), evaluator))
                .ean13(r(row, idx.get(cfg.getEan13()), evaluator))
                .upc12(r(row, idx.get(cfg.getUpc12()), evaluator))
                .hsCode(r(row, idx.get(cfg.getHsCode()), evaluator))
                .countryOfOrigin(r(row, idx.get(cfg.getCountryOfOrigin()), evaluator))
                .weightG(r(row, idx.get(cfg.getWeightG()), evaluator))
                .pkgQty(r(row, idx.get(cfg.getPkgQty()), evaluator))
                .cartonQty(r(row, idx.get(cfg.getCartonQty()), evaluator))
                .stockStatus(r(row, idx.get(cfg.getStockStatus()), evaluator))
                .stockQty(r(row, idx.get(cfg.getStockQty()), evaluator))
                .typeNote(r(row, idx.get(cfg.getTypeNote()), evaluator))
                .build());
        }
        return result;
    }

    /**
     * Возвращает заголовки из файла, не распознанные конфигом.
     *
     * @param sheet лист Excel
     * @return список неизвестных заголовков (отсортированный)
     */
    public List<String> unknownHeaders(Sheet sheet) {
        ExcelImportV4Properties.Columns cfg = props.getColumns();
        Row headerRow = sheet.getRow(props.getHeaderRow() - 1);
        ColumnIndex idx = new ColumnIndex(headerRow);

        Set<String> configured = new HashSet<>(Arrays.asList(
            cfg.getToolNo(), cfg.getAltToolNo(), cfg.getName(),
            cfg.getShortDescription(), cfg.getLongDescription(),
            cfg.getProductType(), cfg.getCategory(), cfg.getGroupName(),
            cfg.getStatus(), cfg.getOrderable(), cfg.getCatalogPage(),
            cfg.getDMm(), cfg.getD1Mm(), cfg.getD2Mm(),
            cfg.getBMm(), cfg.getB1Mm(), cfg.getLMm(), cfg.getL1Mm(),
            cfg.getRMm(), cfg.getAMm(), cfg.getAngleDeg(),
            cfg.getShankMm(), cfg.getShankInch(), cfg.getFlutes(), cfg.getBladeNo(),
            cfg.getCuttingType(), cfg.getRotationDirection(), cfg.getBoreType(),
            cfg.getBallBearing(), cfg.getHasBallBearing(),
            cfg.getRetainer(), cfg.getHasRetainer(), cfg.getCanResharpen(),
            cfg.getToolMaterials(), cfg.getWorkpieceMaterials(),
            cfg.getMachineTypes(), cfg.getMachineBrands(), cfg.getApplicationTags(),
            cfg.getEan13(), cfg.getUpc12(), cfg.getHsCode(), cfg.getCountryOfOrigin(),
            cfg.getWeightG(), cfg.getPkgQty(), cfg.getCartonQty(),
            cfg.getStockStatus(), cfg.getStockQty(), cfg.getTypeNote()
        ));

        return idx.foundHeaders().stream()
            .filter(h -> !configured.contains(h))
            .sorted()
            .toList();
    }

    private static String r(Row row, int col, FormulaEvaluator ev) {
        return CellReader.read(row, col, ev);
    }
}
