package com.wpw.pim.service.excel.parser;

import org.apache.poi.ss.usermodel.*;

/**
 * Утилита для чтения ячеек Excel в строку.
 * Использует FormulaEvaluator для корректного вычисления формул (IF, RIGHT и т.д.).
 */
public final class CellReader {

    private CellReader() {}

    /**
     * Читает ячейку с заданным индексом. FormulaEvaluator может быть null —
     * тогда формулы читаются по кэшированному значению.
     */
    public static String read(Row row, int colIdx, FormulaEvaluator evaluator) {
        if (colIdx < 0 || row == null) return null;
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        CellValue cv = (evaluator != null && cell.getCellType() == CellType.FORMULA)
            ? evaluator.evaluate(cell)
            : null;

        String value;
        if (cv != null) {
            // Результат вычисленной формулы
            value = switch (cv.getCellType()) {
                case NUMERIC -> doubleToString(cv.getNumberValue());
                case STRING  -> cv.getStringValue().trim();
                case BOOLEAN -> String.valueOf(cv.getBooleanValue());
                default      -> null;
            };
        } else {
            value = switch (cell.getCellType()) {
                case NUMERIC -> doubleToString(cell.getNumericCellValue());
                case STRING  -> cell.getStringCellValue().trim();
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> {
                    // Fallback: кэшированный результат без evaluator
                    CellType t = cell.getCachedFormulaResultType();
                    if (t == CellType.NUMERIC) yield doubleToString(cell.getNumericCellValue());
                    if (t == CellType.STRING)  yield cell.getStringCellValue().trim();
                    yield null;
                }
                default -> null;
            };
        }

        return (value == null || value.isBlank()) ? null : value;
    }

    private static String doubleToString(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
