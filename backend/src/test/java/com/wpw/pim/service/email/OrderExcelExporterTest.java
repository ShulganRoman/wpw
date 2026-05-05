package com.wpw.pim.service.email;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.order.Order;
import com.wpw.pim.domain.order.OrderItem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderExcelExporterTest {

    private final OrderExcelExporter exporter = new OrderExcelExporter();

    @Test
    void producesValidXlsxWithCorrectHeaders() throws Exception {
        byte[] bytes = exporter.export(buildOrder(UUID.randomUUID()), Collections.emptyMap());

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("SKU");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Quantity");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("Price");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Image");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Total");
        }
    }

    @Test
    void writesItemDataCorrectly() throws Exception {
        UUID productId = UUID.randomUUID();
        byte[] bytes = exporter.export(buildOrder(productId), Collections.emptyMap());

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("T001");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("2");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("50.00");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("100.00");
        }
    }

    @Test
    void fillsImageUrlWhenPresent() throws Exception {
        UUID productId = UUID.randomUUID();
        String imageUrl = "https://example.com/img.webp";
        byte[] bytes = exporter.export(buildOrder(productId), Map.of(productId, imageUrl));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo(imageUrl);
        }
    }

    @Test
    void producesOneDataRowPerItem() throws Exception {
        Order order = buildOrder(UUID.randomUUID());
        OrderItem second = new OrderItem();
        second.setOrder(order);
        second.setProductId(UUID.randomUUID());
        second.setToolNo("T002");
        second.setName("Tool 2");
        second.setQty(1);
        second.setUnitPrice(new BigDecimal("25.00"));
        second.setLineTotal(new BigDecimal("25.00"));
        order.getItems().add(second);

        byte[] bytes = exporter.export(order, Collections.emptyMap());

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2); // header + 2 items
        }
    }

    private Order buildOrder(UUID productId) {
        Dealer d = new Dealer();
        d.setId(UUID.randomUUID());
        d.setName("Acme");

        Order o = new Order();
        o.setId(UUID.randomUUID());
        o.setDealer(d);
        o.setCurrency("USD");
        o.setTotal(new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProductId(productId);
        item.setToolNo("T001");
        item.setName("Tool 1");
        item.setQty(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        o.getItems().add(item);
        return o;
    }
}
