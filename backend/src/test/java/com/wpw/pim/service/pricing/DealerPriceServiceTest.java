package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.pricing.DealerPriceListDto;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link DealerPriceService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DealerPriceServiceTest {

    @Mock private DealerRepository dealerRepo;
    @Mock private PriceListRepository priceListRepo;
    @Mock private PriceListItemRepository itemRepo;
    @Mock private CurrencyRepository currencyRepo;
    @Mock private ProductRepository productRepo;

    @InjectMocks
    private DealerPriceService service;

    private Currency usd() {
        Currency c = new Currency();
        c.setCode("USD");
        c.setSymbol("$");
        return c;
    }

    private PriceList priceList(UUID id, LocalDate validTo) {
        PriceList pl = new PriceList();
        pl.setId(id);
        pl.setName("Dealer-PL");
        pl.setType("dealer");
        pl.setCurrency(usd());
        pl.setValidFrom(LocalDate.of(2024, 1, 1));
        pl.setValidTo(validTo);
        return pl;
    }

    private Product product(UUID id, String toolNo) {
        Product p = new Product();
        p.setId(id);
        p.setToolNo(toolNo);
        return p;
    }

    @Nested
    @DisplayName("getForDealer")
    class GetForDealer {

        @Test
        @DisplayName("возвращает прайс-лист дилера, не expired")
        void returnsActive() {
            UUID dealerId = UUID.randomUUID();
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            PriceList pl = priceList(plId, LocalDate.now().plusYears(1));
            dealer.setPriceList(pl);

            PriceListItem item = new PriceListItem();
            item.setId(new PriceListItemId(plId, prodId, 1));
            item.setProduct(product(prodId, "T-1"));
            item.setPrice(BigDecimal.valueOf(10));

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of(item));

            DealerPriceListDto result = service.getForDealer(dealerId);

            assertThat(result).isNotNull();
            assertThat(result.expired()).isFalse();
            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("expired=true если validTo в прошлом")
        void expiredFlag() {
            UUID dealerId = UUID.randomUUID();
            UUID plId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            PriceList pl = priceList(plId, LocalDate.now().minusDays(1));
            dealer.setPriceList(pl);

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of());

            DealerPriceListDto result = service.getForDealer(dealerId);
            assertThat(result.expired()).isTrue();
        }

        @Test
        @DisplayName("возвращает null если у дилера нет прайс-листа")
        void noPriceList() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            dealer.setPriceList(null);
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));

            assertThat(service.getForDealer(dealerId)).isNull();
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void notFound() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getForDealer(dealerId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("importPriceList")
    class ImportPriceListT {

        @Test
        @DisplayName("создаёт новый прайс-лист и сохраняет позиции")
        void createsNewList() throws IOException {
            UUID dealerId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            dealer.setPriceList(null);

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(currencyRepo.findById("USD")).thenReturn(Optional.of(usd()));
            when(productRepo.findByToolNo("TOOL-1")).thenReturn(Optional.of(product(prodId, "TOOL-1")));
            when(productRepo.findByToolNo("TOOL-MISS")).thenReturn(Optional.empty());
            when(priceListRepo.save(any(PriceList.class))).thenAnswer(inv -> {
                PriceList pl = inv.getArgument(0);
                if (pl.getId() == null) pl.setId(UUID.randomUUID());
                return pl;
            });

            byte[] xlsx = buildExcel();
            MultipartFile file = new MockMultipartFile("file", "p.xlsx",
                "application/vnd.ms-excel", xlsx);

            PriceImportResult result = service.importPriceList(dealerId, file, "USD", LocalDate.now().plusYears(1));

            assertThat(result.imported()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            verify(priceListRepo).save(any(PriceList.class));
            verify(dealerRepo).save(dealer);
        }

        @Test
        @DisplayName("переиспользует существующий прайс-лист дилера")
        void reusesExisting() throws IOException {
            UUID dealerId = UUID.randomUUID();
            UUID plId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            PriceList existing = priceList(plId, null);
            dealer.setPriceList(existing);

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(currencyRepo.findById("USD")).thenReturn(Optional.of(usd()));
            when(productRepo.findByToolNo(any())).thenReturn(Optional.empty());
            when(priceListRepo.save(existing)).thenReturn(existing);

            byte[] xlsx = buildExcel();
            MultipartFile file = new MockMultipartFile("file", "p.xlsx",
                "application/vnd.ms-excel", xlsx);

            PriceImportResult result = service.importPriceList(dealerId, file, "USD", null);

            assertThat(result.imported()).isEqualTo(0);
            verify(itemRepo).deleteByPriceListId(plId);
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void noDealer() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.empty());
            MultipartFile file = new MockMultipartFile("f", new byte[]{1});
            assertThatThrownBy(() -> service.importPriceList(dealerId, file, "USD", null))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("400 если валюта не найдена")
        void unknownCurrency() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(currencyRepo.findById("XXX")).thenReturn(Optional.empty());
            MultipartFile file = new MockMultipartFile("f", new byte[]{1});
            assertThatThrownBy(() -> service.importPriceList(dealerId, file, "XXX", null))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("deletePriceList")
    class DeletePriceListT {

        @Test
        @DisplayName("удаляет прайс-лист дилера")
        void deletes() {
            UUID dealerId = UUID.randomUUID();
            UUID plId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            PriceList pl = priceList(plId, null);
            dealer.setPriceList(pl);

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));

            service.deletePriceList(dealerId);

            assertThat(dealer.getPriceList()).isNull();
            verify(itemRepo).deleteByPriceListId(plId);
            verify(priceListRepo).delete(pl);
        }

        @Test
        @DisplayName("ничего не делает если прайс-лист отсутствует")
        void noOp() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            dealer.setPriceList(null);
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));

            service.deletePriceList(dealerId);

            verify(itemRepo, never()).deleteByPriceListId(any());
            verify(priceListRepo, never()).delete(any(PriceList.class));
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void notFound() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deletePriceList(dealerId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("export & template")
    class ExportAndTemplate {

        @Test
        @DisplayName("export -- успешно собирает Excel")
        void exportsBytes() throws IOException {
            UUID dealerId = UUID.randomUUID();
            UUID plId = UUID.randomUUID();
            UUID prodId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            PriceList pl = priceList(plId, null);
            dealer.setPriceList(pl);
            PriceListItem item = new PriceListItem();
            item.setId(new PriceListItemId(plId, prodId, 1));
            item.setProduct(product(prodId, "TOOL-1"));
            item.setPrice(BigDecimal.valueOf(11));

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(itemRepo.findByPriceListIdOrderByIdMinQtyAsc(plId)).thenReturn(List.of(item));

            byte[] result = service.export(dealerId);

            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) 'P');
        }

        @Test
        @DisplayName("export -- 404 если у дилера нет прайс-листа")
        void exportNoList() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            dealer.setPriceList(null);
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));

            assertThatThrownBy(() -> service.export(dealerId))
                .isInstanceOf(ResponseStatusException.class);
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

    private byte[] buildExcel() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Price List");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("tool_no");
            h.createCell(1).setCellValue("min_qty");
            h.createCell(2).setCellValue("price");
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("TOOL-1");
            r1.createCell(1).setCellValue(1);
            r1.createCell(2).setCellValue(10.5);
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("TOOL-MISS");
            r2.createCell(1).setCellValue(1);
            r2.createCell(2).setCellValue(20.0);
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("");
            r3.createCell(1).setCellValue(1);
            r3.createCell(2).setCellValue(0);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
