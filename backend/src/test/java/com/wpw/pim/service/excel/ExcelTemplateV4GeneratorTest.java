package com.wpw.pim.service.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelTemplateV4GeneratorTest {

    private final ExcelTemplateV4Generator generator = new ExcelTemplateV4Generator();

    @Test
    @DisplayName("generate returns non-null bytes")
    void generate_notNull() throws Exception {
        byte[] result = generator.generate();
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generated workbook contains Products sheet")
    void generate_hasProductsSheet() throws Exception {
        byte[] result = generator.generate();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Products");
            assertThat(sheet).isNotNull();
        }
    }

    @Test
    @DisplayName("header row 2 contains Tool No")
    void generate_headerContainsToolNo() throws Exception {
        byte[] result = generator.generate();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Products");
            Row headerRow = sheet.getRow(1); // 0-based → row 2
            assertThat(headerRow).isNotNull();
            boolean found = false;
            for (int i = 0; i <= headerRow.getLastCellNum(); i++) {
                if (headerRow.getCell(i) != null
                    && "Tool No".equals(headerRow.getCell(i).getStringCellValue())) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    @DisplayName("header row 2 contains Group Name but not Group ID")
    void generate_headerContainsGroupNameNotGroupId() throws Exception {
        byte[] result = generator.generate();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Products");
            Row headerRow = sheet.getRow(1);
            boolean hasGroupName = false;
            boolean hasGroupId = false;
            for (int i = 0; i <= headerRow.getLastCellNum(); i++) {
                if (headerRow.getCell(i) != null) {
                    String val = headerRow.getCell(i).getStringCellValue();
                    if ("Group Name".equals(val)) hasGroupName = true;
                    if ("Group ID".equals(val)) hasGroupId = true;
                }
            }
            assertThat(hasGroupName).isTrue();
            assertThat(hasGroupId).isFalse();
        }
    }

    @Test
    @DisplayName("example row 3 contains data")
    void generate_exampleRowHasData() throws Exception {
        byte[] result = generator.generate();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Products");
            Row exampleRow = sheet.getRow(2); // 0-based → row 3
            assertThat(exampleRow).isNotNull();
            // First cell should be the example tool number
            assertThat(exampleRow.getCell(0).getStringCellValue()).isEqualTo("DT12702");
        }
    }
}
