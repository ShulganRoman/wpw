package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.config.ExcelImportV4Properties;
import com.wpw.pim.service.excel.dto.RawV4Row;
import com.wpw.pim.service.excel.parser.V4SheetParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class V4SheetParserTest {

    private final ExcelImportV4Properties props = new ExcelImportV4Properties();
    private final V4SheetParser parser = new V4SheetParser(props);

    private Sheet createSheet(XSSFWorkbook wb, String[] headers, String[]... dataRows) {
        Sheet sheet = wb.createSheet("Products");
        // Header row at index 1 (row 2 in 1-based)
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        // Data rows start at index 2 (row 3 in 1-based)
        for (int r = 0; r < dataRows.length; r++) {
            Row dataRow = sheet.createRow(2 + r);
            for (int c = 0; c < dataRows[r].length; c++) {
                if (dataRows[r][c] != null) {
                    dataRow.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
        }
        return sheet;
    }

    @Test
    @DisplayName("parses row with data - fields are populated")
    void parse_returnsPopulatedRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Alt Tool No", "Name", "Short Description",
                "Long Description", "Product Type", "Category", "Group Name",
                "Status", "Orderable", "D (mm)", "Flutes", "Cutting Type",
                "Tool Materials", "Application Tags"};
            String[] row1 = {"DT12702", "ALT-01", "Spiral Brazed Bit", "Short desc",
                "Long desc", "main", "Router Bits", "Spiral Brazed Bits",
                "active", "yes", "12.7", "2", "straight",
                "K-Carbide", "grooving, trimming"};

            Sheet sheet = createSheet(wb, headers, row1);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawV4Row> rows = parser.parse(sheet, evaluator);

            assertThat(rows).hasSize(1);
            RawV4Row row = rows.get(0);
            assertThat(row.getToolNo()).isEqualTo("DT12702");
            assertThat(row.getAltToolNo()).isEqualTo("ALT-01");
            assertThat(row.getName()).isEqualTo("Spiral Brazed Bit");
            assertThat(row.getShortDescription()).isEqualTo("Short desc");
            assertThat(row.getLongDescription()).isEqualTo("Long desc");
            assertThat(row.getProductType()).isEqualTo("main");
            assertThat(row.getCategoryName()).isEqualTo("Router Bits");
            assertThat(row.getGroupName()).isEqualTo("Spiral Brazed Bits");
            assertThat(row.getStatus()).isEqualTo("active");
            assertThat(row.getOrderable()).isEqualTo("yes");
            assertThat(row.getDMm()).isEqualTo("12.7");
            assertThat(row.getFlutes()).isEqualTo("2");
            assertThat(row.getCuttingType()).isEqualTo("straight");
            assertThat(row.getToolMaterials()).isEqualTo("K-Carbide");
            assertThat(row.getApplicationTags()).isEqualTo("grooving, trimming");
            assertThat(row.getRowNum()).isEqualTo(3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("row without toolNo is skipped")
    void parse_skipsRowWithoutToolNo() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Name", "Category"};
            String[] row1 = {null, "Some Name", "Router Bits"};

            Sheet sheet = createSheet(wb, headers, row1);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawV4Row> rows = parser.parse(sheet, evaluator);
            assertThat(rows).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("empty rows are skipped")
    void parse_skipsEmptyRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Name"};
            Sheet sheet = createSheet(wb, headers);
            sheet.createRow(2); // empty data row
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawV4Row> rows = parser.parse(sheet, evaluator);
            assertThat(rows).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("unknownHeaders returns headers not in config")
    void unknownHeaders_returnsUnknown() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Name", "Extra Column", "Mystery Field"};
            Sheet sheet = createSheet(wb, headers);

            List<String> unknown = parser.unknownHeaders(sheet);

            assertThat(unknown).containsExactly("Extra Column", "Mystery Field");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
