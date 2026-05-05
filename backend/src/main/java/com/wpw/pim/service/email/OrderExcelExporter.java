package com.wpw.pim.service.email;

import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderItem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@Component
public class OrderExcelExporter {

    private record ColumnDef(String header, BiFunction<OrderItem, Map<UUID, String>, String> value) {
    }

    // ── Edit this list to change columns, their order, or their content ──
    private static final List<ColumnDef> COLUMNS = List.of(
            new ColumnDef("SKU",      (item, imgs) -> item.getToolNo()),
            new ColumnDef("Quantity", (item, imgs) -> String.valueOf(item.getQty())),
            new ColumnDef("Price",    (item, imgs) -> item.getUnitPrice() != null ? item.getUnitPrice().toPlainString() : ""),
            new ColumnDef("Image",    (item, imgs) -> imgs.getOrDefault(item.getProductId(), "")),
            new ColumnDef("Total",    (item, imgs) -> item.getLineTotal() != null ? item.getLineTotal().toPlainString() : "")
    );

    public byte[] export(Order order, Map<UUID, String> imageUrlByProductId) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Order");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) {
                headerRow.createCell(i).setCellValue(COLUMNS.get(i).header());
            }

            int rowNum = 1;
            for (OrderItem item : order.getItems()) {
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < COLUMNS.size(); col++) {
                    String val = COLUMNS.get(col).value().apply(item, imageUrlByProductId);
                    row.createCell(col).setCellValue(val != null ? val : "");
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build order Excel", e);
        }
    }
}
