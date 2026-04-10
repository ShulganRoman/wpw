package com.wpw.pim.service.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ExcelTemplateGenerator {

    private static final String[] PRODUCTS_HEADERS = {
        "Tool No", "Alt Tool No", "Name", "Short Description", "Long Description",
        "Category", "Group ID", "Group Name", "Status", "Orderable", "Product Type",
        "Catalog Page", "D (mm)", "D1 (mm)", "D2 (mm)", "B / Cut. Length (mm)", "B1 (mm)",
        "L / Total (mm)", "L1 (mm)", "R (mm)", "A (mm)", "Angle (\u00b0)",
        "Shank (mm)", "Shank (in)", "Flutes", "Blade No", "Cutting Type",
        "Rotation Direction", "Bore Type", "Ball Bearing", "Has Ball Bearing",
        "Retainer", "Has Retainer", "Can Resharpen",
        "Tool Materials", "Workpiece Materials", "Machine Types", "Machine Brands",
        "Application Tags", "Materials", "Machines",
        "EAN-13", "UPC-12", "HS Code", "Country of Origin",
        "Weight (g)", "Package Qty", "Carton Qty", "Stock Status", "Stock Qty",
        "Type / Note"
    };

    private static final String[] GROUPS_HEADERS = {
        "Group ID", "Group Code", "Group Name", "Category",
        "Short Description", "Typical Applications",
        "Materials", "Machines / Equipment", "Catalog Pages"
    };

    public byte[] generate() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle instructionStyle = createInstructionStyle(wb);

            createProductsSheet(wb, headerStyle, instructionStyle);
            createGroupsSheet(wb, headerStyle, instructionStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void createProductsSheet(XSSFWorkbook wb, CellStyle headerStyle, CellStyle instructionStyle) {
        Sheet sheet = wb.createSheet("Products");

        // Row 1: instruction
        Row instrRow = sheet.createRow(0);
        Cell instrCell = instrRow.createCell(0);
        instrCell.setCellValue("WPW PIM Import Template \u2014 fill in data starting from row 3");
        instrCell.setCellStyle(instructionStyle);

        // Row 2: headers
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < PRODUCTS_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PRODUCTS_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Row 3: example data (DT12702)
        Row exampleRow = sheet.createRow(2);
        int col = 0;
        exampleRow.createCell(col++).setCellValue("DT12702");          // Tool No
        exampleRow.createCell(col++);                                   // Alt Tool No
        exampleRow.createCell(col++).setCellValue("WPW DT12702 Spiral Brazed Bit D12.7 B25 L151 Z2 d12 K-Carbide"); // Name
        exampleRow.createCell(col++);                                   // Short Description
        exampleRow.createCell(col++).setCellValue("The WPW DT12702 is a spiral brazed router bit designed for clean cuts in wood composites."); // Long Description
        exampleRow.createCell(col++).setCellValue("Router Bits");       // Category
        exampleRow.createCell(col++).setCellValue("spiral-brazed-bits"); // Group ID
        exampleRow.createCell(col++).setCellValue("Spiral Brazed Bits"); // Group Name
        exampleRow.createCell(col++).setCellValue("active");            // Status
        exampleRow.createCell(col++).setCellValue("yes");               // Orderable
        exampleRow.createCell(col++).setCellValue("main");              // Product Type
        exampleRow.createCell(col++);                                   // Catalog Page
        exampleRow.createCell(col++).setCellValue("12.7");              // D (mm)
        exampleRow.createCell(col++);                                   // D1 (mm)
        exampleRow.createCell(col++);                                   // D2 (mm)
        exampleRow.createCell(col++).setCellValue("25");                // B / Cut. Length (mm)
        exampleRow.createCell(col++);                                   // B1 (mm)
        exampleRow.createCell(col++).setCellValue("151");               // L / Total (mm)
        exampleRow.createCell(col++);                                   // L1 (mm)
        exampleRow.createCell(col++);                                   // R (mm)
        exampleRow.createCell(col++);                                   // A (mm)
        exampleRow.createCell(col++);                                   // Angle
        exampleRow.createCell(col++).setCellValue("12");                // Shank (mm)
        exampleRow.createCell(col++);                                   // Shank (in)
        exampleRow.createCell(col++).setCellValue("2");                 // Flutes
        exampleRow.createCell(col++);                                   // Blade No
        exampleRow.createCell(col++).setCellValue("straight");          // Cutting Type
        exampleRow.createCell(col++);                                   // Rotation Direction
        exampleRow.createCell(col++);                                   // Bore Type
        exampleRow.createCell(col++);                                   // Ball Bearing
        exampleRow.createCell(col++);                                   // Has Ball Bearing
        exampleRow.createCell(col++);                                   // Retainer
        exampleRow.createCell(col++);                                   // Has Retainer
        exampleRow.createCell(col++);                                   // Can Resharpen
        exampleRow.createCell(col++).setCellValue("K-Carbide");         // Tool Materials
        exampleRow.createCell(col++).setCellValue("Veneer, Painted_Panels, Chipboard, Laminated_Chipboard, Plywood, HPL, Laminated_MDF, Hardwood, Softwood, MDF"); // Workpiece Materials
        exampleRow.createCell(col++).setCellValue("Table Router, CNC, Handheld Router"); // Machine Types
        exampleRow.createCell(col++);                                   // Machine Brands
        exampleRow.createCell(col++).setCellValue("plunge-cutting, trimming, grooving, nesting, contour-cutting"); // Application Tags
        exampleRow.createCell(col++);                                   // Materials (legacy)
        exampleRow.createCell(col++);                                   // Machines (legacy)
        exampleRow.createCell(col++);                                   // EAN-13
        exampleRow.createCell(col++);                                   // UPC-12
        exampleRow.createCell(col++);                                   // HS Code
        exampleRow.createCell(col++);                                   // Country of Origin
        exampleRow.createCell(col++);                                   // Weight (g)
        exampleRow.createCell(col++).setCellValue("1");                 // Package Qty
        exampleRow.createCell(col++);                                   // Carton Qty
        exampleRow.createCell(col++).setCellValue("out_of_stock");      // Stock Status
        exampleRow.createCell(col++);                                   // Stock Qty
        exampleRow.createCell(col);                                     // Type / Note

        sheet.createFreezePane(0, 2);
    }

    private void createGroupsSheet(XSSFWorkbook wb, CellStyle headerStyle, CellStyle instructionStyle) {
        Sheet sheet = wb.createSheet("Product Groups");

        // Row 1: instruction
        Row instrRow = sheet.createRow(0);
        Cell instrCell = instrRow.createCell(0);
        instrCell.setCellValue("WPW PIM Import Template \u2014 fill in group data starting from row 3");
        instrCell.setCellStyle(instructionStyle);

        // Row 2: headers
        Row headerRow = sheet.createRow(1);
        for (int i = 0; i < GROUPS_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(GROUPS_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Row 3: example data
        Row exampleRow = sheet.createRow(2);
        exampleRow.createCell(0).setCellValue("spiral-brazed-bits");
        exampleRow.createCell(1).setCellValue("SBB");
        exampleRow.createCell(2).setCellValue("Spiral Brazed Bits");
        exampleRow.createCell(3).setCellValue("Router Bits");

        sheet.createFreezePane(0, 2);
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
