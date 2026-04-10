package com.wpw.pim.service.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Генератор Excel-шаблона формата v4 (один лист "Products", без Group ID).
 * <p>
 * Порядок колонок строго соответствует шаблону пользователя:
 * Tool No, Alt Tool No, Name, ... Type / Note.
 * </p>
 */
@Service
public class ExcelTemplateV4Generator {

    private static final String[] HEADERS = {
        "Tool No", "Alt Tool No", "Name", "Short Description", "Long Description",
        "Product Type", "Category", "Group Name", "Status", "Orderable", "Catalog Page",
        "D (mm)", "D1 (mm)", "D2 (mm)", "B / Cut. Length (mm)", "B1 (mm)",
        "L / Total (mm)", "L1 (mm)", "R (mm)", "A (mm)", "Angle (\u00b0)",
        "Shank (mm)", "Shank (in)", "Flutes", "Blade No",
        "Cutting Type", "Rotation Direction", "Bore Type",
        "Ball Bearing", "Has Ball Bearing", "Retainer", "Has Retainer", "Can Resharpen",
        "Tool Materials", "Workpiece Materials", "Machine Types", "Machine Brands",
        "Application Tags",
        "EAN-13", "UPC-12", "HS Code", "Country of Origin",
        "Weight (g)", "Package Qty", "Carton Qty", "Stock Status", "Stock Qty",
        "Type / Note"
    };

    /**
     * Генерирует Excel-шаблон v4 с инструкцией, заголовками и примером.
     *
     * @return байтовый массив .xlsx файла
     */
    public byte[] generate() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle instructionStyle = createInstructionStyle(wb);

            Sheet sheet = wb.createSheet("Products");

            // Row 1: инструкция
            Row instrRow = sheet.createRow(0);
            Cell instrCell = instrRow.createCell(0);
            instrCell.setCellValue("WPW PIM Import v4 \u2014 fill in products starting from row 3. "
                + "Groups are created automatically from Category + Group Name columns.");
            instrCell.setCellStyle(instructionStyle);

            // Row 2: заголовки
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Row 3: пример данных
            Row exampleRow = sheet.createRow(2);
            int col = 0;
            exampleRow.createCell(col++).setCellValue("DT12702");           // Tool No
            exampleRow.createCell(col++);                                    // Alt Tool No
            exampleRow.createCell(col++).setCellValue("WPW DT12702 Spiral Brazed Bit D12.7 B25 L151 Z2 d12 K-Carbide"); // Name
            exampleRow.createCell(col++).setCellValue("Spiral brazed router bit, 12.7mm, K-Carbide"); // Short Description
            exampleRow.createCell(col++).setCellValue("The WPW DT12702 is a spiral brazed router bit designed for clean cuts in wood composites."); // Long Description
            exampleRow.createCell(col++).setCellValue("main");              // Product Type
            exampleRow.createCell(col++).setCellValue("Router Bits");       // Category
            exampleRow.createCell(col++).setCellValue("Spiral Brazed Bits"); // Group Name
            exampleRow.createCell(col++).setCellValue("active");            // Status
            exampleRow.createCell(col++).setCellValue("yes");               // Orderable
            exampleRow.createCell(col++);                                    // Catalog Page
            exampleRow.createCell(col++).setCellValue("12.7");              // D (mm)
            exampleRow.createCell(col++);                                    // D1 (mm)
            exampleRow.createCell(col++);                                    // D2 (mm)
            exampleRow.createCell(col++).setCellValue("25");                // B / Cut. Length (mm)
            exampleRow.createCell(col++);                                    // B1 (mm)
            exampleRow.createCell(col++).setCellValue("151");               // L / Total (mm)
            exampleRow.createCell(col++);                                    // L1 (mm)
            exampleRow.createCell(col++);                                    // R (mm)
            exampleRow.createCell(col++);                                    // A (mm)
            exampleRow.createCell(col++);                                    // Angle
            exampleRow.createCell(col++).setCellValue("12");                // Shank (mm)
            exampleRow.createCell(col++);                                    // Shank (in)
            exampleRow.createCell(col++).setCellValue("2");                 // Flutes
            exampleRow.createCell(col++);                                    // Blade No
            exampleRow.createCell(col++).setCellValue("straight");          // Cutting Type
            exampleRow.createCell(col++);                                    // Rotation Direction
            exampleRow.createCell(col++);                                    // Bore Type
            exampleRow.createCell(col++);                                    // Ball Bearing
            exampleRow.createCell(col++);                                    // Has Ball Bearing
            exampleRow.createCell(col++);                                    // Retainer
            exampleRow.createCell(col++);                                    // Has Retainer
            exampleRow.createCell(col++);                                    // Can Resharpen
            exampleRow.createCell(col++).setCellValue("K-Carbide");         // Tool Materials
            exampleRow.createCell(col++).setCellValue("MDF, Hardwood, Chipboard, HPL"); // Workpiece Materials
            exampleRow.createCell(col++).setCellValue("CNC, Table Router"); // Machine Types
            exampleRow.createCell(col++);                                    // Machine Brands
            exampleRow.createCell(col++).setCellValue("grooving, trimming, nesting"); // Application Tags
            exampleRow.createCell(col++);                                    // EAN-13
            exampleRow.createCell(col++);                                    // UPC-12
            exampleRow.createCell(col++);                                    // HS Code
            exampleRow.createCell(col++);                                    // Country of Origin
            exampleRow.createCell(col++);                                    // Weight (g)
            exampleRow.createCell(col++).setCellValue("1");                 // Package Qty
            exampleRow.createCell(col++);                                    // Carton Qty
            exampleRow.createCell(col++).setCellValue("out_of_stock");      // Stock Status
            exampleRow.createCell(col++);                                    // Stock Qty
            exampleRow.createCell(col);                                      // Type / Note

            // Заморозить первые 2 строки (инструкция + заголовки)
            sheet.createFreezePane(0, 2);

            // Ширины колонок в единицах 1/256 символа (autoSizeColumn недоступен в headless-среде)
            int[] widths = {
                3500,  // Tool No
                3500,  // Alt Tool No
                12000, // Name
                7000,  // Short Description
                14000, // Long Description
                3000,  // Product Type
                4000,  // Category
                5000,  // Group Name
                3000,  // Status
                3000,  // Orderable
                3500,  // Catalog Page
                2800,  // D (mm)
                2800,  // D1 (mm)
                2800,  // D2 (mm)
                5000,  // B / Cut. Length (mm)
                2800,  // B1 (mm)
                4500,  // L / Total (mm)
                2800,  // L1 (mm)
                2800,  // R (mm)
                2800,  // A (mm)
                3000,  // Angle (°)
                3500,  // Shank (mm)
                3500,  // Shank (in)
                2800,  // Flutes
                2800,  // Blade No
                4000,  // Cutting Type
                5000,  // Rotation Direction
                3000,  // Bore Type
                4000,  // Ball Bearing
                4500,  // Has Ball Bearing
                3500,  // Retainer
                3800,  // Has Retainer
                4000,  // Can Resharpen
                6000,  // Tool Materials
                12000, // Workpiece Materials
                6000,  // Machine Types
                6000,  // Machine Brands
                8000,  // Application Tags
                3800,  // EAN-13
                3500,  // UPC-12
                3500,  // HS Code
                5000,  // Country of Origin
                3500,  // Weight (g)
                3500,  // Package Qty
                3500,  // Carton Qty
                4000,  // Stock Status
                3500,  // Stock Qty
                5000,  // Type / Note
            };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x1E, (byte) 0x3A, (byte) 0x5F}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createInstructionStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
