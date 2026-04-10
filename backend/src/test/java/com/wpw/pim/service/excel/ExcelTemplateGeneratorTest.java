package com.wpw.pim.service.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelTemplateGeneratorTest {

    private final ExcelTemplateGenerator generator = new ExcelTemplateGenerator();

    @Test
    @DisplayName("generate returns non-null bytes")
    void generate_returnsBytes() throws Exception {
        byte[] bytes = generator.generate();
        assertThat(bytes).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generated workbook has Products sheet")
    void generate_hasProductsSheet() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheet("Products")).isNotNull();
        }
    }

    @Test
    @DisplayName("generated workbook has Product Groups sheet")
    void generate_hasGroupsSheet() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheet("Product Groups")).isNotNull();
        }
    }

    @Test
    @DisplayName("Products header row has Tool No column")
    void generate_productsHeader_hasToolNo() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Products");
            Row headerRow = sheet.getRow(1);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Tool No");
        }
    }

    @Test
    @DisplayName("Products header row has Application Tags column")
    void generate_productsHeader_hasApplicationTags() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Products");
            Row headerRow = sheet.getRow(1);
            boolean found = false;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                if ("Application Tags".equals(headerRow.getCell(i).getStringCellValue())) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    @DisplayName("Products header row has Name column")
    void generate_productsHeader_hasName() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Products");
            Row headerRow = sheet.getRow(1);
            boolean found = false;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                if ("Name".equals(headerRow.getCell(i).getStringCellValue())) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    @DisplayName("example row has DT12702 as Tool No")
    void generate_exampleRow_hasDT12702() throws Exception {
        byte[] bytes = generator.generate();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Products");
            Row exampleRow = sheet.getRow(2);
            assertThat(exampleRow.getCell(0).getStringCellValue()).isEqualTo("DT12702");
        }
    }
}
