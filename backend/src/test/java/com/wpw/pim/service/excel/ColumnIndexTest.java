package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.parser.ColumnIndex;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link ColumnIndex}.
 * Проверяют построение индекса колонок из строки заголовков Excel.
 */
class ColumnIndexTest {

    private static Workbook workbook;
    private static Sheet sheet;

    @BeforeAll
    static void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Tool No");
        headerRow.createCell(1).setCellValue("Description");
        headerRow.createCell(2).setCellValue("D (mm)");
        headerRow.createCell(3).setCellValue(""); // пустая ячейка
        headerRow.createCell(5).setCellValue("L (mm)"); // gap: col 4 пропущен
    }

    @AfterAll
    static void tearDown() throws Exception {
        workbook.close();
    }

    @Test
    @DisplayName("get — существующий заголовок возвращает правильный индекс")
    void get_existingHeader_returnsCorrectIndex() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));

        assertThat(idx.get("Tool No")).isEqualTo(0);
        assertThat(idx.get("Description")).isEqualTo(1);
        assertThat(idx.get("D (mm)")).isEqualTo(2);
        assertThat(idx.get("L (mm)")).isEqualTo(5);
    }

    @Test
    @DisplayName("get — несуществующий заголовок возвращает -1")
    void get_missingHeader_returnsMinusOne() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));
        assertThat(idx.get("Unknown Header")).isEqualTo(-1);
    }

    @Test
    @DisplayName("has — существующий заголовок возвращает true")
    void has_existingHeader_returnsTrue() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));
        assertThat(idx.has("Tool No")).isTrue();
    }

    @Test
    @DisplayName("has — несуществующий заголовок возвращает false")
    void has_missingHeader_returnsFalse() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));
        assertThat(idx.has("Unknown")).isFalse();
    }

    @Test
    @DisplayName("foundHeaders — содержит все непустые заголовки")
    void foundHeaders_containsAllNonEmptyHeaders() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));
        assertThat(idx.foundHeaders())
            .containsExactlyInAnyOrder("Tool No", "Description", "D (mm)", "L (mm)");
    }

    @Test
    @DisplayName("constructor — null headerRow не выбрасывает исключение")
    void constructor_nullRow_doesNotThrow() {
        ColumnIndex idx = new ColumnIndex(null);
        assertThat(idx.get("Tool No")).isEqualTo(-1);
        assertThat(idx.foundHeaders()).isEmpty();
    }

    @Test
    @DisplayName("foundHeaders — пустая строка в ячейке игнорируется")
    void foundHeaders_emptyCell_ignored() {
        ColumnIndex idx = new ColumnIndex(sheet.getRow(0));
        // Ячейка col 3 пустая — не должна быть в foundHeaders
        assertThat(idx.foundHeaders()).doesNotContain("");
    }
}
