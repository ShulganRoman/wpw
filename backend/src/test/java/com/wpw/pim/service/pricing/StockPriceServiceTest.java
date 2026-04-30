package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link StockPriceService}.
 * Покрывают CRUD операции, импорт/экспорт Excel, получение шаблона.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockPriceServiceTest {

    @Mock private PriceListRepository priceListRepo;
    @Mock private PriceListItemRepository itemRepo;
    @Mock private ProductRepository productRepo;

    @InjectMocks
    private StockPriceService service;

    private PriceList stockList(UUID id) {
        PriceList pl = new PriceList();
        pl.setId(id);
        pl.setName("Stock");
        pl.setType("stock");
        Currency cur = new Currency();
        cur.setCode("USD");
        cur.setSymbol("$");
        pl.setCurrency(cur);
        return pl;
    }

    private Product product(UUID id, String toolNo) {
        Product p = new Product();
        p.setId(id);
        p.setToolNo(toolNo);
        return p;
    }

    @Nested
    @DisplayName("getOrCreateStockList")
    class GetOrCreateStockList {

        @Test
        @DisplayName("возвращает существующий stock-лист")
        void returnsExisting() {
            PriceList existing = stockList(UUID.randomUUID());
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(existing));

            PriceList result = service.getOrCreateStockList();

            assertThat(result).isSameAs(existing);
            verify(priceListRepo, never()).save(any());
        }

        @Test
        @DisplayName("создаёт новый stock-лист если его нет")
        void createsNew() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            when(priceListRepo.save(any(PriceList.class))).thenAnswer(inv -> inv.getArgument(0));

            PriceList result = service.getOrCreateStockList();

            assertThat(result.getType()).isEqualTo("stock");
            assertThat(result.getName()).isEqualTo("Stock");
            verify(priceListRepo).save(any(PriceList.class));
        }
    }

    @Nested
    @DisplayName("getItems")
    class GetItems {

        @Test
        @DisplayName("возвращает позиции прайс-листа")
        void returnsItems() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");
            PriceListItem item = new PriceListItem();
            item.setId(new PriceListItemId(plId, prodId, 1));
            item.setProduct(p);
            item.setPrice(BigDecimal.valueOf(10.50));

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of(item));

            List<PriceListItemDto> result = service.getItems();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).toolNo()).isEqualTo("TOOL-001");
            assertThat(result.get(0).minQty()).isEqualTo(1);
            assertThat(result.get(0).price()).isEqualByComparingTo("10.50");
        }

        @Test
        @DisplayName("бросает 404 если stock-лист не инициализирован")
        void throwsNotFound() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getItems())
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("upsertItem")
    class UpsertItem {

        @Test
        @DisplayName("создаёт новую позицию")
        void createsNew() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(p));
            when(itemRepo.findById(any(PriceListItemId.class))).thenReturn(Optional.empty());

            PriceListItemRequest req = new PriceListItemRequest("TOOL-001", 1, BigDecimal.valueOf(20));
            PriceListItemDto result = service.upsertItem(req);

            assertThat(result.toolNo()).isEqualTo("TOOL-001");
            verify(itemRepo).save(any(PriceListItem.class));
        }

        @Test
        @DisplayName("обновляет существующую позицию")
        void updatesExisting() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");
            PriceListItem existing = new PriceListItem();
            existing.setId(new PriceListItemId(plId, prodId, 1));
            existing.setPrice(BigDecimal.ONE);

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(p));
            when(itemRepo.findById(any(PriceListItemId.class))).thenReturn(Optional.of(existing));

            PriceListItemRequest req = new PriceListItemRequest("TOOL-001", 1, BigDecimal.valueOf(99));
            service.upsertItem(req);

            assertThat(existing.getPrice()).isEqualByComparingTo("99");
            verify(itemRepo).save(existing);
        }

        @Test
        @DisplayName("404 если stock-лист не инициализирован")
        void throwsIfNoStock() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.upsertItem(
                new PriceListItemRequest("TOOL-001", 1, BigDecimal.ONE)))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("404 если продукт не найден")
        void throwsIfNoProduct() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(UUID.randomUUID())));
            when(productRepo.findByToolNo("TOOL-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.upsertItem(
                new PriceListItemRequest("TOOL-X", 1, BigDecimal.ONE)))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItem {

        @Test
        @DisplayName("удаляет позицию")
        void deletes() {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(p));

            service.deleteItem("TOOL-001", 1);

            verify(itemRepo).deleteById(new PriceListItemId(plId, prodId, 1));
        }

        @Test
        @DisplayName("404 если stock-лист отсутствует")
        void noStock() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteItem("TOOL-001", 1))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("404 если продукт не найден")
        void noProduct() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(stockList(UUID.randomUUID())));
            when(productRepo.findByToolNo("TOOL-Y")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteItem("TOOL-Y", 1))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("importExcel")
    class ImportExcel {

        @Test
        @DisplayName("успешно импортирует строки и репортит ненайденные продукты")
        void importsRows() throws IOException {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(p));
            when(productRepo.findByToolNo("TOOL-MISS")).thenReturn(Optional.empty());

            byte[] xlsx = buildExcel(new String[][]{
                {"tool_no", "min_qty", "price"},
                {"TOOL-001", "1", "10.5"},
                {"TOOL-MISS", "1", "20.0"},
                {"", "1", "1"}, // blank — skipped
            });
            MultipartFile file = new MockMultipartFile("file", "p.xlsx",
                "application/vnd.ms-excel", xlsx);

            PriceImportResult result = service.importExcel(file);

            assertThat(result.imported()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            verify(itemRepo).deleteByPriceListId(plId);
            verify(itemRepo).saveAll(any());
        }

        @Test
        @DisplayName("обрабатывает строку с числовым tool_no")
        void importsNumericToolNo() throws IOException {
            UUID plId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(productRepo.findByToolNo(any())).thenReturn(Optional.empty());

            byte[] xlsx = buildExcelMixed();
            MultipartFile file = new MockMultipartFile("file", "p.xlsx",
                "application/vnd.ms-excel", xlsx);

            PriceImportResult result = service.importExcel(file);

            assertThat(result.imported()).isEqualTo(0);
            // skipped/errors will be > 0 since not found
        }

        @Test
        @DisplayName("404 если stock-лист не инициализирован")
        void noStock() {
            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.empty());
            MultipartFile file = new MockMultipartFile("file", new byte[]{1, 2, 3});
            assertThatThrownBy(() -> service.importExcel(file))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("export & template")
    class ExportAndTemplate {

        @Test
        @DisplayName("export -- возвращает Excel байты")
        void exportsExcel() throws IOException {
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            PriceList pl = stockList(plId);
            Product p = product(prodId, "TOOL-001");
            PriceListItem item = new PriceListItem();
            item.setId(new PriceListItemId(plId, prodId, 1));
            item.setProduct(p);
            item.setPrice(BigDecimal.valueOf(15));

            when(priceListRepo.findFirstByType("stock")).thenReturn(Optional.of(pl));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of(item));

            byte[] result = service.export();

            assertThat(result).isNotEmpty();
            // valid xlsx should start with PK signature
            assertThat(result[0]).isEqualTo((byte) 'P');
            assertThat(result[1]).isEqualTo((byte) 'K');
        }

        @Test
        @DisplayName("template -- возвращает Excel шаблон")
        void templateBytes() throws IOException {
            byte[] result = service.template();

            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) 'P');
            assertThat(result[1]).isEqualTo((byte) 'K');
        }
    }

    // --- helpers ---

    private byte[] buildExcel(String[][] rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stock Prices");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                String[] cols = rows[r];
                row.createCell(0).setCellValue(cols[0]);
                if (r == 0) {
                    row.createCell(1).setCellValue(cols[1]);
                    row.createCell(2).setCellValue(cols[2]);
                } else {
                    if (!cols[1].isBlank()) row.createCell(1).setCellValue(Double.parseDouble(cols[1]));
                    if (!cols[2].isBlank()) row.createCell(2).setCellValue(Double.parseDouble(cols[2]));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildExcelMixed() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stock Prices");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("tool_no");
            h.createCell(1).setCellValue("min_qty");
            h.createCell(2).setCellValue("price");
            // numeric tool_no — exercises NUMERIC branch in cellStr
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(12345);
            r1.createCell(1).setCellValue(1);
            r1.createCell(2).setCellValue(10.0);
            // null row to test null guard
            // row index 3 — left null
            wb.write(out);
            return out.toByteArray();
        }
    }
}
