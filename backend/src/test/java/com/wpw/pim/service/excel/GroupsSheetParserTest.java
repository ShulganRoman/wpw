package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.RawGroupRow;
import com.wpw.pim.service.excel.parser.GroupsSheetParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupsSheetParserTest {

    private final ExcelImportProperties props = new ExcelImportProperties();
    private final GroupsSheetParser parser = new GroupsSheetParser(props);

    private Sheet createGroupSheet(XSSFWorkbook wb, String[] headers, String[]... dataRows) {
        Sheet sheet = wb.createSheet("Product Groups");
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
    @DisplayName("parses group rows from Excel sheet")
    void parse_returnsGroupRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Group ID", "Group Code", "Group Name", "Category",
                    "Short Description", "Typical Applications", "Materials",
                    "Machines / Equipment", "Catalog Pages"};
            String[] row1 = {"GRP-01", "straight-bits", "Straight Bits", "Router Bits",
                    "For straight cuts", "Grooving, Dadoing", "HM, HSS",
                    "CNC, Router", "42-45"};
            String[] row2 = {"GRP-02", "profile-bits", "Profile Bits", "Router Bits",
                    "For profiling", "Edge profiling", "HM",
                    "Router", "50"};

            Sheet sheet = createGroupSheet(wb, headers, row1, row2);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawGroupRow> rows = parser.parse(sheet, evaluator);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).getGroupId()).isEqualTo("GRP-01");
            assertThat(rows.get(0).getGroupCode()).isEqualTo("straight-bits");
            assertThat(rows.get(0).getGroupName()).isEqualTo("Straight Bits");
            assertThat(rows.get(0).getCategoryName()).isEqualTo("Router Bits");
            assertThat(rows.get(0).getShortDescription()).isEqualTo("For straight cuts");
            assertThat(rows.get(0).getApplications()).isEqualTo("Grooving, Dadoing");
            assertThat(rows.get(0).getMaterials()).isEqualTo("HM, HSS");
            assertThat(rows.get(0).getMachines()).isEqualTo("CNC, Router");
            assertThat(rows.get(0).getCatalogPages()).isEqualTo("42-45");
            assertThat(rows.get(0).getRowNum()).isEqualTo(3);

            assertThat(rows.get(1).getGroupId()).isEqualTo("GRP-02");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("skips rows with null group ID")
    void parse_skipsNullGroupId() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Group ID", "Group Name"};
            String[] row1 = {null, "Some Group"};

            Sheet sheet = createGroupSheet(wb, headers, row1);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawGroupRow> rows = parser.parse(sheet, evaluator);
            assertThat(rows).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("handles empty sheet with only headers")
    void parse_emptyData_returnsEmptyList() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Group ID", "Group Name"};
            Sheet sheet = createGroupSheet(wb, headers);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawGroupRow> rows = parser.parse(sheet, evaluator);
            assertThat(rows).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
