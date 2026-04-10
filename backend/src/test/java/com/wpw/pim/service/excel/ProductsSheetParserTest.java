package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.RawProductRow;
import com.wpw.pim.service.excel.parser.ProductsSheetParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductsSheetParserTest {

    private final ExcelImportProperties props = new ExcelImportProperties();
    private final ProductsSheetParser parser = new ProductsSheetParser(props);

    private Sheet createSheet(XSSFWorkbook wb, String[] headers, String[]... dataRows) {
        Sheet sheet = wb.createSheet("Products");
        // Header row at index 1 (row 2 in 1-based, matching default headerRow=2)
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        // Data rows start at index 2 (row 3 in 1-based, matching default dataStartRow=3)
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
    @DisplayName("parses product rows from Excel sheet")
    void parse_returnsProductRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Alt Tool No", "Category", "Group ID", "Group Name",
                    "Description", "D (mm)", "D1 (mm)", "B / Cut. Length (mm)", "B1 (mm)",
                    "L / Total (mm)", "L1 (mm)", "R (mm)", "A (mm)", "Angle (\u00b0)",
                    "Shank (mm)", "Shank (in)", "Flutes", "Cutting Type", "Ball Bearing",
                    "Retainer", "Blade No", "Materials", "Applications", "Machines",
                    "Type / Note", "Catalog Page",
                    "Name", "Short Description", "Long Description", "Status", "Orderable",
                    "Product Type", "D2 (mm)", "Rotation Direction", "Bore Type",
                    "Has Ball Bearing", "Has Retainer", "Can Resharpen",
                    "Tool Materials", "Workpiece Materials", "Machine Types", "Machine Brands",
                    "Application Tags", "EAN-13", "UPC-12", "HS Code", "Country of Origin",
                    "Weight (g)", "Package Qty", "Carton Qty", "Stock Status", "Stock Qty"};
            String[] row1 = {"WPW-001", "ALT-001", "Router Bits", "GRP-01", "Straight Bits",
                    "Straight router bit", "12.5", "8.0", "25", null,
                    "60", null, null, null, null,
                    "8", "5/16", "2", "straight", null,
                    null, null, "HM", "Grooving", "CNC",
                    "Standard", "42",
                    "My Product Name", "Short desc", "Long desc", "active", "yes",
                    "main", "6.5", "right", "shank",
                    "yes", "no", "yes",
                    "K-Carbide", "MDF, Plywood", "CNC, Router", "Festool",
                    "plunge-cutting, trimming", "1234567890123", "123456789012", "8207.70", "Israel",
                    "150", "1", "10", "in_stock", "500"};
            String[] row2 = {"WPW-002", null, "Router Bits", "GRP-01", "Straight Bits",
                    "Another bit", "10", null, "20", null,
                    "50", null, null, null, null,
                    "12", "1/2", "3", "spiral", null,
                    null, null, "HSS", "Profiling", "Router",
                    null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null};

            Sheet sheet = createSheet(wb, headers, row1, row2);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawProductRow> rows = parser.parse(sheet, evaluator);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).getToolNo()).isEqualTo("WPW-001");
            assertThat(rows.get(0).getAltToolNo()).isEqualTo("ALT-001");
            assertThat(rows.get(0).getCategoryName()).isEqualTo("Router Bits");
            assertThat(rows.get(0).getGroupId()).isEqualTo("GRP-01");
            assertThat(rows.get(0).getDMm()).isEqualTo("12.5");
            assertThat(rows.get(0).getShankMm()).isEqualTo("8");
            assertThat(rows.get(0).getFlutes()).isEqualTo("2");
            assertThat(rows.get(0).getCatalogPage()).isEqualTo("42");
            assertThat(rows.get(0).getRowNum()).isEqualTo(3);
            assertThat(rows.get(0).getName()).isEqualTo("My Product Name");
            assertThat(rows.get(0).getShortDescription()).isEqualTo("Short desc");
            assertThat(rows.get(0).getLongDescription()).isEqualTo("Long desc");
            assertThat(rows.get(0).getStatus()).isEqualTo("active");
            assertThat(rows.get(0).getOrderable()).isEqualTo("yes");
            assertThat(rows.get(0).getProductType()).isEqualTo("main");
            assertThat(rows.get(0).getD2Mm()).isEqualTo("6.5");
            assertThat(rows.get(0).getRotationDirection()).isEqualTo("right");
            assertThat(rows.get(0).getBoreType()).isEqualTo("shank");
            assertThat(rows.get(0).getHasBallBearing()).isEqualTo("yes");
            assertThat(rows.get(0).getHasRetainer()).isEqualTo("no");
            assertThat(rows.get(0).getCanResharpen()).isEqualTo("yes");
            assertThat(rows.get(0).getToolMaterials()).isEqualTo("K-Carbide");
            assertThat(rows.get(0).getWorkpieceMaterials()).isEqualTo("MDF, Plywood");
            assertThat(rows.get(0).getMachineTypes()).isEqualTo("CNC, Router");
            assertThat(rows.get(0).getMachineBrands()).isEqualTo("Festool");
            assertThat(rows.get(0).getApplicationTags()).isEqualTo("plunge-cutting, trimming");
            assertThat(rows.get(0).getEan13()).isEqualTo("1234567890123");
            assertThat(rows.get(0).getUpc12()).isEqualTo("123456789012");
            assertThat(rows.get(0).getHsCode()).isEqualTo("8207.70");
            assertThat(rows.get(0).getCountryOfOrigin()).isEqualTo("Israel");
            assertThat(rows.get(0).getWeightG()).isEqualTo("150");
            assertThat(rows.get(0).getPkgQty()).isEqualTo("1");
            assertThat(rows.get(0).getCartonQty()).isEqualTo("10");
            assertThat(rows.get(0).getStockStatus()).isEqualTo("in_stock");
            assertThat(rows.get(0).getStockQty()).isEqualTo("500");

            assertThat(rows.get(1).getToolNo()).isEqualTo("WPW-002");
            assertThat(rows.get(1).getAltToolNo()).isNull();
            assertThat(rows.get(1).getName()).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("skips empty rows")
    void parse_skipsEmptyRows() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Group ID"};
            Sheet sheet = createSheet(wb, headers);
            // Add an empty row at index 2
            sheet.createRow(2);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<RawProductRow> rows = parser.parse(sheet, evaluator);
            assertThat(rows).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("unknownHeaders returns headers not in config")
    void unknownHeaders_returnsUnknownOnes() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Group ID", "Extra Column", "Unknown Header"};
            Sheet sheet = createSheet(wb, headers);
            var evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<String> unknown = parser.unknownHeaders(sheet);

            assertThat(unknown).containsExactly("Extra Column", "Unknown Header");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("all known headers produce empty unknownHeaders list")
    void unknownHeaders_allKnown_returnsEmpty() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Tool No", "Category", "Group ID", "Description"};
            Sheet sheet = createSheet(wb, headers);

            List<String> unknown = parser.unknownHeaders(sheet);
            assertThat(unknown).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
