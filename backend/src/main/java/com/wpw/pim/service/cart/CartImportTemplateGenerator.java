package com.wpw.pim.service.cart;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Generates the Excel template for bulk cart import.
 * <p>
 * To change template columns: edit COLUMNS and update the example row below.
 */
@Service
public class CartImportTemplateGenerator {

    // ── Template definition ───────────────────────────────────────────────────
    private static final String[] COLUMNS = {"Tool No",   // A — WPW article (required)
            "Quantity",  // B — integer ≥ 1 (required)
    };

    private static final Object[] EXAMPLE_ROW = {"ATP5005D",  // A
            10,          // B
    };
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generate() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cart Import");

            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle exampleStyle = buildExampleStyle(wb);

            // Row 0: instruction
            Row instr = sheet.createRow(0);
            Cell instrCell = instr.createCell(0);
            instrCell.setCellValue(
                    "WPW Cart Import — fill in Tool No (WPW article) and Quantity starting from row 3. Row 2 is an example.");
            instrCell.setCellStyle(buildInstructionStyle(wb));

            // Row 1: headers
            Row header = sheet.createRow(1);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(COLUMNS[i]);
                c.setCellStyle(headerStyle);
            }

            // Row 2: example
            Row example = sheet.createRow(2);
            for (int i = 0; i < EXAMPLE_ROW.length; i++) {
                Cell c = example.createCell(i);
                Object val = EXAMPLE_ROW[i];
                if (val instanceof Number n) c.setCellValue(n.doubleValue());
                else c.setCellValue(String.valueOf(val));
                c.setCellStyle(exampleStyle);
            }

            // Auto-size columns
//            for (int i = 0; i < COLUMNS.length; i++) {
//                sheet.autoSizeColumn(i);
//                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 4000));
//            }
            sheet.setColumnWidth(0, 5000); // Tool No
            sheet.setColumnWidth(1, 3000); // Quantity

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle buildExampleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle buildInstructionStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
