package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.dto.WpwCatalogRow;
import com.wpw.pim.service.excel.parser.WpwCatalogParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WpwCatalogParserTest {

    private final WpwCatalogParser parser = new WpwCatalogParser();

    private static final String[] ALL_HEADERS = {
            "SKU", "Product_Type", "Category", "Group",
            "Description_EN", "Name_RU", "AI_Description_EN",
            "D_mm", "D1_mm", "B_mm", "L_mm", "R_mm", "Angle_deg",
            "Shank_mm", "Shank_inch", "Flutes",
            "Tool_Material", "Cutting_Type", "Application_Tags",
            "Workpiece_Material", "Machine_Type",
            "Data_Completeness", "Spare_Parts", "Related_Products", "Set_Components"
    };

    private Sheet createSheet(XSSFWorkbook wb, String[] headers, String[]... dataRows) {
        Sheet sheet = wb.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int r = 0; r < dataRows.length; r++) {
            Row dataRow = sheet.createRow(1 + r);
            for (int c = 0; c < dataRows[r].length; c++) {
                if (dataRows[r][c] != null) {
                    dataRow.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
        }
        return sheet;
    }

    @Test
    @DisplayName("parses all columns from WPW catalog sheet")
    void parse_allColumns() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] row1 = {
                    "WPW-001", "main", "Router Bits", "Straight",
                    "Straight bit 12mm", "Prjamaja freza", "AI description",
                    "12.5", "8.0", "25", "60", "3.5", "15",
                    "8", "5/16", "2",
                    "HM", "straight", "Grooving, Profiling",
                    "Wood|MDF", "CNC|Router",
                    "complete", "SP-001", "WPW-002", "SET-001"
            };

            Sheet sheet = createSheet(wb, ALL_HEADERS, row1);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<WpwCatalogRow> rows = parser.parse(sheet, evaluator);

            assertThat(rows).hasSize(1);
            WpwCatalogRow r = rows.get(0);
            assertThat(r.getRowNum()).isEqualTo(2);
            assertThat(r.getSku()).isEqualTo("WPW-001");
            assertThat(r.getProductType()).isEqualTo("main");
            assertThat(r.getCategory()).isEqualTo("Router Bits");
            assertThat(r.getGroup()).isEqualTo("Straight");
            assertThat(r.getDescriptionEn()).isEqualTo("Straight bit 12mm");
            assertThat(r.getNameRu()).isEqualTo("Prjamaja freza");
            assertThat(r.getAiDescriptionEn()).isEqualTo("AI description");
            assertThat(r.getDMm()).isEqualTo("12.5");
            assertThat(r.getD1Mm()).isEqualTo("8.0");
            assertThat(r.getBMm()).isEqualTo("25");
            assertThat(r.getLMm()).isEqualTo("60");
            assertThat(r.getRMm()).isEqualTo("3.5");
            assertThat(r.getAngleDeg()).isEqualTo("15");
            assertThat(r.getShankMm()).isEqualTo("8");
            assertThat(r.getShankInch()).isEqualTo("5/16");
            assertThat(r.getFlutes()).isEqualTo("2");
            assertThat(r.getToolMaterial()).isEqualTo("HM");
            assertThat(r.getCuttingType()).isEqualTo("straight");
            assertThat(r.getApplicationTags()).isEqualTo("Grooving, Profiling");
            assertThat(r.getWorkpieceMaterial()).isEqualTo("Wood|MDF");
            assertThat(r.getMachineType()).isEqualTo("CNC|Router");
            assertThat(r.getDataCompleteness()).isEqualTo("complete");
            assertThat(r.getSpareParts()).isEqualTo("SP-001");
            assertThat(r.getRelatedProducts()).isEqualTo("WPW-002");
            assertThat(r.getSetComponents()).isEqualTo("SET-001");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("skips rows with blank SKU")
    void parse_skipsBlankSku() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] row1 = {null, "main", "Bits", "Group"};
            String[] row2 = {"", "main", "Bits", "Group"};
            String[] row3 = {"WPW-002", "main", "Bits", "Group", "Valid bit"};

            Sheet sheet = createSheet(wb, ALL_HEADERS, row1, row2, row3);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<WpwCatalogRow> rows = parser.parse(sheet, evaluator);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getSku()).isEqualTo("WPW-002");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("unknownHeaders detects non-standard columns")
    void unknownHeaders_detectsExtra() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"SKU", "Category", "Extra_Column", "Another_Unknown"};
            Sheet sheet = createSheet(wb, headers);

            List<String> unknown = parser.unknownHeaders(sheet);
            assertThat(unknown).containsExactly("Another_Unknown", "Extra_Column");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("unknownHeaders ignores known SEO columns")
    void unknownHeaders_ignoresSeoColumns() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"SKU", "SEO_Title_EN", "SEO_Description_EN", "Keywords_EN", "URL_Slug"};
            Sheet sheet = createSheet(wb, headers);

            List<String> unknown = parser.unknownHeaders(sheet);
            assertThat(unknown).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("parses multiple data rows")
    void parse_multipleRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] row1 = {"WPW-001", "main", "Bits", "Straight", "Bit 1"};
            String[] row2 = {"WPW-002", "spare_part", "Bits", "Profile", "Bit 2"};
            String[] row3 = {"WPW-003", "accessory", "Clamps", "Quick", "Clamp"};

            Sheet sheet = createSheet(wb, ALL_HEADERS, row1, row2, row3);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<WpwCatalogRow> rows = parser.parse(sheet, evaluator);
            assertThat(rows).hasSize(3);
            assertThat(rows.get(0).getSku()).isEqualTo("WPW-001");
            assertThat(rows.get(1).getSku()).isEqualTo("WPW-002");
            assertThat(rows.get(2).getSku()).isEqualTo("WPW-003");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
