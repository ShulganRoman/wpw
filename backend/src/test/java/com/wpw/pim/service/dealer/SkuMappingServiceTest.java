package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link SkuMappingService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkuMappingServiceTest {

    @Mock private DealerSkuMappingRepository mappingRepo;
    @Mock private DealerRepository dealerRepo;
    @Mock private ProductRepository productRepo;

    @InjectMocks
    private SkuMappingService service;

    @Nested
    @DisplayName("list")
    class ListT {

        @Test
        @DisplayName("возвращает маппинги дилера")
        void returnsList() {
            UUID dealerId = UUID.randomUUID();
            DealerSkuMapping m = new DealerSkuMapping();
            m.setId(new DealerSkuMappingId(dealerId, "WPW-1"));
            m.setDealerSku("D-1");
            m.setDealerBrand("BX");

            when(mappingRepo.findByDealerId(dealerId)).thenReturn(List.of(m));

            List<SkuMappingDto> result = service.list(dealerId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).wpwSku()).isEqualTo("WPW-1");
        }
    }

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Test
        @DisplayName("создаёт новый маппинг")
        void createsNew() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(mappingRepo.findById(any(DealerSkuMappingId.class))).thenReturn(Optional.empty());

            SkuMappingDto req = new SkuMappingDto("WPW-1", "D-1", "BX");
            SkuMappingDto result = service.upsert(dealerId, req);

            assertThat(result.dealerSku()).isEqualTo("D-1");
            verify(mappingRepo).save(any(DealerSkuMapping.class));
        }

        @Test
        @DisplayName("обновляет существующий маппинг")
        void updatesExisting() {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            DealerSkuMapping existing = new DealerSkuMapping();
            existing.setId(new DealerSkuMappingId(dealerId, "WPW-1"));

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(mappingRepo.findById(any())).thenReturn(Optional.of(existing));

            service.upsert(dealerId, new SkuMappingDto("WPW-1", "NEW", "BR"));

            assertThat(existing.getDealerSku()).isEqualTo("NEW");
            verify(mappingRepo).save(existing);
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void noDealer() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.upsert(dealerId, new SkuMappingDto("a", "b", null)))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteT {

        @Test
        @DisplayName("удаляет существующий маппинг")
        void deletes() {
            UUID dealerId = UUID.randomUUID();
            DealerSkuMappingId id = new DealerSkuMappingId(dealerId, "WPW-1");
            when(mappingRepo.existsById(id)).thenReturn(true);

            service.delete(dealerId, "WPW-1");
            verify(mappingRepo).deleteById(id);
        }

        @Test
        @DisplayName("404 если маппинг не существует")
        void notFound() {
            UUID dealerId = UUID.randomUUID();
            when(mappingRepo.existsById(any())).thenReturn(false);

            assertThatThrownBy(() -> service.delete(dealerId, "X"))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("классифицирует строки на valid/ghosts/errors")
        void classifies() throws IOException {
            when(productRepo.existsByToolNo("WPW-1")).thenReturn(true);
            when(productRepo.existsByToolNo("WPW-GHOST")).thenReturn(false);

            byte[] xlsx = buildExcel(new String[][]{
                {"wpw_sku", "dealer_sku", "dealer_brand"},
                {"WPW-1", "D-1", "BX"},
                {"WPW-GHOST", "D-2", null},
                {"", "no-wpw", null},        // empty wpw → error
                {"WPW-NO-DEALER", "", null}, // empty dealer → error
                {"WPW-1", "D-DUP", null}     // duplicate
            });
            MultipartFile file = new MockMultipartFile("file", "f.xlsx",
                "application/vnd.ms-excel", xlsx);

            SkuMappingService.ValidationReport report = service.validate(file);

            assertThat(report.total()).isGreaterThanOrEqualTo(3);
            assertThat(report.valid()).isNotEmpty();
            assertThat(report.ghosts()).hasSize(1);
            assertThat(report.errors()).isNotEmpty();
        }

        @Test
        @DisplayName("обрабатывает разные типы ячеек (numeric, boolean)")
        void handlesCellTypes() throws IOException {
            when(productRepo.existsByToolNo(any())).thenReturn(true);

            byte[] xlsx = buildExcelWithMixedTypes();
            MultipartFile file = new MockMultipartFile("file", "f.xlsx",
                "application/vnd.ms-excel", xlsx);

            SkuMappingService.ValidationReport report = service.validate(file);
            assertThat(report.total()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("импортирует с skipGhosts=true")
        void executeSkipGhosts() throws IOException {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(productRepo.existsByToolNo("WPW-1")).thenReturn(true);
            when(productRepo.existsByToolNo("WPW-GHOST")).thenReturn(false);
            when(mappingRepo.existsById(any())).thenReturn(false);

            byte[] xlsx = buildExcel(new String[][]{
                {"wpw_sku", "dealer_sku", "dealer_brand"},
                {"WPW-1", "D-1", "BX"},
                {"WPW-GHOST", "D-G", null},
            });
            MultipartFile file = new MockMultipartFile("file", "f.xlsx",
                "application/vnd.ms-excel", xlsx);

            SkuMappingService.SkuMappingImportResult result = service.execute(dealerId, file, true);

            assertThat(result.imported()).isEqualTo(1);
            assertThat(result.created()).isEqualTo(1);
            assertThat(result.ghostCount()).isEqualTo(1);
            assertThat(result.skippedGhosts()).contains("WPW-GHOST");
        }

        @Test
        @DisplayName("импортирует с skipGhosts=false (включая ghosts)")
        void executeIncludeGhosts() throws IOException {
            UUID dealerId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);

            DealerSkuMapping existing = new DealerSkuMapping();
            existing.setId(new DealerSkuMappingId(dealerId, "WPW-1"));

            when(dealerRepo.findById(dealerId)).thenReturn(Optional.of(dealer));
            when(productRepo.existsByToolNo("WPW-1")).thenReturn(true);
            when(productRepo.existsByToolNo("WPW-GHOST")).thenReturn(false);
            when(mappingRepo.existsById(new DealerSkuMappingId(dealerId, "WPW-1"))).thenReturn(true);
            when(mappingRepo.findById(new DealerSkuMappingId(dealerId, "WPW-1"))).thenReturn(Optional.of(existing));
            when(mappingRepo.existsById(new DealerSkuMappingId(dealerId, "WPW-GHOST"))).thenReturn(false);

            byte[] xlsx = buildExcel(new String[][]{
                {"wpw_sku", "dealer_sku", "dealer_brand"},
                {"WPW-1", "D-1", "BX"},
                {"WPW-GHOST", "D-G", null},
            });
            MultipartFile file = new MockMultipartFile("file", "f.xlsx",
                "application/vnd.ms-excel", xlsx);

            SkuMappingService.SkuMappingImportResult result = service.execute(dealerId, file, false);

            assertThat(result.imported()).isEqualTo(2);
            assertThat(result.created()).isEqualTo(1);
            assertThat(result.updated()).isEqualTo(1);
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void noDealer() {
            UUID dealerId = UUID.randomUUID();
            when(dealerRepo.findById(dealerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(dealerId,
                new MockMultipartFile("f", new byte[]{1}), true))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("export & template")
    class ExportAndTemplate {

        @Test
        @DisplayName("export возвращает Excel байты")
        void exportsBytes() throws IOException {
            UUID dealerId = UUID.randomUUID();
            DealerSkuMapping m = new DealerSkuMapping();
            m.setId(new DealerSkuMappingId(dealerId, "WPW-1"));
            m.setDealerSku("D-1");
            m.setDealerBrand("BX");

            when(mappingRepo.findByDealerId(dealerId)).thenReturn(List.of(m));

            byte[] result = service.export(dealerId);
            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) 'P');
            assertThat(result[1]).isEqualTo((byte) 'K');
        }

        @Test
        @DisplayName("template возвращает Excel шаблон")
        void templateBytes() throws IOException {
            byte[] result = service.template();
            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) 'P');
        }
    }

    // --- helpers ---

    private byte[] buildExcel(String[][] rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SKU Mapping");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    String value = rows[r][c];
                    if (value != null) row.createCell(c).setCellValue(value);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildExcelWithMixedTypes() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SKU Mapping");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("wpw_sku");
            h.createCell(1).setCellValue("dealer_sku");
            h.createCell(2).setCellValue("dealer_brand");

            // numeric integer wpw_sku
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(12345); // numeric int
            r1.createCell(1).setCellValue("D-1");
            r1.createCell(2).setCellValue("Brand");

            // numeric float
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(123.45);
            r2.createCell(1).setCellValue(true); // boolean
            r2.createCell(2).setCellValue("");

            // empty row -- skipped
            sheet.createRow(3);

            wb.write(out);
            return out.toByteArray();
        }
    }
}
