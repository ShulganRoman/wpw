package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.parser.CellReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link CellReader}.
 * Создаёт тестовый Workbook в памяти с Apache POI для проверки чтения различных типов ячеек.
 */
class CellReaderTest {

    private static Workbook workbook;
    private static Sheet sheet;
    private static FormulaEvaluator evaluator;

    @BeforeAll
    static void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test");
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        // Row 0: разные типы ячеек
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Hello");           // STRING
        row0.createCell(1).setCellValue(42.0);              // NUMERIC (целое)
        row0.createCell(2).setCellValue(3.14);              // NUMERIC (дробное)
        row0.createCell(3).setCellValue(true);              // BOOLEAN
        row0.createCell(4).setCellValue("");                 // пустая строка
        row0.createCell(5).setCellValue("   ");             // пробелы
        // col 6 — отсутствует (null)

        // Row 1: формулы
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellFormula("1+1");           // формула → число
        row1.createCell(1).setCellFormula("\"AB\" & \"CD\""); // формула → строка
        row1.createCell(2).setCellFormula("TRUE");           // формула → boolean

        // Row 2: NaN/infinity — это нельзя поставить в ячейку напрямую,
        // но можно проверить через doubleToString
    }

    @AfterAll
    static void tearDown() throws Exception {
        workbook.close();
    }

    @Test
    @DisplayName("read — STRING ячейка возвращает trimmed строку")
    void read_stringCell_returnsTrimmedValue() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 0, evaluator)).isEqualTo("Hello");
    }

    @Test
    @DisplayName("read — NUMERIC целое число возвращается без дробной части")
    void read_numericIntegerCell_returnsWithoutDecimal() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 1, evaluator)).isEqualTo("42");
    }

    @Test
    @DisplayName("read — NUMERIC дробное число возвращается как есть")
    void read_numericDecimalCell_returnsDecimalString() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 2, evaluator)).isEqualTo("3.14");
    }

    @Test
    @DisplayName("read — BOOLEAN ячейка возвращает 'true' или 'false'")
    void read_booleanCell_returnsBooleanString() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 3, evaluator)).isEqualTo("true");
    }

    @Test
    @DisplayName("read — пустая строка возвращает null")
    void read_emptyStringCell_returnsNull() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 4, evaluator)).isNull();
    }

    @Test
    @DisplayName("read — строка из пробелов возвращает null")
    void read_blankStringCell_returnsNull() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 5, evaluator)).isNull();
    }

    @Test
    @DisplayName("read — отсутствующая ячейка (null) возвращает null")
    void read_missingCell_returnsNull() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 6, evaluator)).isNull();
    }

    @Test
    @DisplayName("read — отрицательный индекс возвращает null")
    void read_negativeIndex_returnsNull() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, -1, evaluator)).isNull();
    }

    @Test
    @DisplayName("read — null row возвращает null")
    void read_nullRow_returnsNull() {
        assertThat(CellReader.read(null, 0, evaluator)).isNull();
    }

    @Test
    @DisplayName("read — формула 1+1 возвращает '2'")
    void read_formulaNumeric_returnsEvaluatedResult() {
        Row row = sheet.getRow(1);
        assertThat(CellReader.read(row, 0, evaluator)).isEqualTo("2");
    }

    @Test
    @DisplayName("read — формула конкатенации возвращает строку")
    void read_formulaString_returnsEvaluatedString() {
        Row row = sheet.getRow(1);
        assertThat(CellReader.read(row, 1, evaluator)).isEqualTo("ABCD");
    }

    @Test
    @DisplayName("read — формула TRUE возвращает 'true'")
    void read_formulaBoolean_returnsBooleanString() {
        Row row = sheet.getRow(1);
        assertThat(CellReader.read(row, 2, evaluator)).isEqualTo("true");
    }

    @Test
    @DisplayName("read — формула без evaluator использует кэшированное значение")
    void read_formulaWithoutEvaluator_usesCachedValue() {
        // Сначала вычислим формулы через evaluator чтобы кэш заполнился
        evaluator.evaluateAll();

        Row row = sheet.getRow(1);
        // Без evaluator — fallback к кэшированному результату
        String result = CellReader.read(row, 0, null);
        assertThat(result).isEqualTo("2");
    }

    @Test
    @DisplayName("read — индекс за пределами строки возвращает null")
    void read_outOfRangeIndex_returnsNull() {
        Row row = sheet.getRow(0);
        assertThat(CellReader.read(row, 100, evaluator)).isNull();
    }
}
