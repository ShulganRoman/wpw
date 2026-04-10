package com.wpw.pim.service.export;

import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.web.dto.product.ProductFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ExportService}.
 * Проверяют экспорт в CSV, XLSX и XML форматы.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock private ProductRepository productRepo;
    @Mock private ProductTranslationRepository translationRepo;
    @Mock private MediaFileRepository mediaFileRepo;

    private ExportService exportService;

    private Product testProduct;
    private ProductTranslation testTranslation;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(productRepo, translationRepo, mediaFileRepo,
            "https://pim.example.com");

        testProduct = new Product();
        testProduct.setId(UUID.randomUUID());
        testProduct.setToolNo("DR001");
        testProduct.setAltToolNo("ALT001");
        testProduct.setProductType(ProductType.main);
        testProduct.setStatus(ProductStatus.active);

        ProductAttributes attrs = new ProductAttributes();
        attrs.setDMm(new BigDecimal("12.5"));
        attrs.setLMm(new BigDecimal("50"));
        attrs.setShankMm(new BigDecimal("8"));
        attrs.setFlutes((short) 2);
        attrs.setCuttingType("straight");
        testProduct.setAttributes(attrs);

        testTranslation = new ProductTranslation();
        testTranslation.setId(new ProductTranslationId(testProduct.getId(), "en"));
        testTranslation.setName("Diamond Router Bit");
        testTranslation.setShortDescription("High quality bit");
    }

    private ProductFilter defaultFilter() {
        return new ProductFilter("en", null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, 1, 48);
    }

    private void mockRepos(List<Product> products) {
        when(productRepo.findAll(any(Specification.class))).thenReturn(products);
        List<UUID> ids = products.stream().map(Product::getId).toList();
        if (!ids.isEmpty()) {
            when(translationRepo.findByProductIdsAndLocale(eq(ids), eq("en")))
                .thenReturn(List.of(testTranslation));
            when(mediaFileRepo.findByProductIds(eq(ids))).thenReturn(Collections.emptyList());
        }
    }

    // ========================= CSV =========================

    @Nested
    @DisplayName("export CSV")
    class ExportCsv {

        @Test
        void export_csv_emptyProducts_returnsHeaderOnly() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of());

            byte[] result = exportService.export("csv", "en", defaultFilter());
            String csv = new String(result);

            assertThat(csv).contains("\"tool_no\"");
            assertThat(csv.lines().count()).isEqualTo(1); // header only
        }

        @Test
        void export_csv_withProduct_containsToolNo() {
            mockRepos(List.of(testProduct));

            byte[] result = exportService.export("csv", "en", defaultFilter());
            String csv = new String(result);

            assertThat(csv).contains("DR001");
            assertThat(csv).contains("Diamond Router Bit");
            assertThat(csv).contains("12.5");
        }

        @Test
        void export_csv_nullTranslation_emptyName() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of(testProduct));
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("en")))
                .thenReturn(List.of());
            when(mediaFileRepo.findByProductIds(anyList())).thenReturn(Collections.emptyList());

            byte[] result = exportService.export("csv", "en", defaultFilter());
            String csv = new String(result);

            assertThat(csv).contains("DR001");
        }

        @Test
        void export_csv_withImages_containsImageUrls() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of(testProduct));
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("en")))
                .thenReturn(List.of(testTranslation));

            MediaFile mf = new MediaFile();
            mf.setProduct(testProduct);
            mf.setUrl("/media/products/DR001/1.webp");
            mf.setSortOrder(0);
            when(mediaFileRepo.findByProductIds(anyList())).thenReturn(List.of(mf));

            byte[] result = exportService.export("csv", "en", defaultFilter());
            String csv = new String(result);

            assertThat(csv).contains("image_url_1");
            assertThat(csv).contains("https://pim.example.com/media/products/DR001/1.webp");
        }
    }

    // ========================= XLSX =========================

    @Nested
    @DisplayName("export XLSX")
    class ExportXlsx {

        @Test
        void export_xlsx_returnsValidBytes() {
            mockRepos(List.of(testProduct));

            byte[] result = exportService.export("xlsx", "en", defaultFilter());

            assertThat(result).isNotEmpty();
            // XLSX файлы начинаются с PK (ZIP-формат)
            assertThat(result[0]).isEqualTo((byte) 0x50); // 'P'
            assertThat(result[1]).isEqualTo((byte) 0x4B); // 'K'
        }

        @Test
        void export_xlsx_emptyProducts_returnsValidFile() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of());

            byte[] result = exportService.export("xlsx", "en", defaultFilter());
            assertThat(result).isNotEmpty();
        }
    }

    // ========================= XML =========================

    @Nested
    @DisplayName("export XML")
    class ExportXml {

        @Test
        void export_xml_containsValidStructure() {
            mockRepos(List.of(testProduct));

            byte[] result = exportService.export("xml", "en", defaultFilter());
            String xml = new String(result);

            assertThat(xml).startsWith("<?xml version=\"1.0\"");
            assertThat(xml).contains("<products>");
            assertThat(xml).contains("<product>");
            assertThat(xml).contains("<tool_no>DR001</tool_no>");
            assertThat(xml).contains("<name>Diamond Router Bit</name>");
            assertThat(xml).contains("<status>active</status>");
            assertThat(xml).contains("</products>");
        }

        @Test
        void export_xml_emptyProducts_returnsValidXml() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of());

            byte[] result = exportService.export("xml", "en", defaultFilter());
            String xml = new String(result);

            assertThat(xml).contains("<products>");
            assertThat(xml).contains("</products>");
            assertThat(xml).doesNotContain("<product>");
        }

        @Test
        void export_xml_escapesSpecialChars() {
            testTranslation.setName("Bit <size> & \"type\"");
            mockRepos(List.of(testProduct));

            byte[] result = exportService.export("xml", "en", defaultFilter());
            String xml = new String(result);

            assertThat(xml).contains("&lt;size&gt;");
            assertThat(xml).contains("&amp;");
        }

        @Test
        void export_xml_withImages_containsImageNodes() {
            when(productRepo.findAll(any(Specification.class))).thenReturn(List.of(testProduct));
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("en")))
                .thenReturn(List.of(testTranslation));

            MediaFile mf = new MediaFile();
            mf.setProduct(testProduct);
            mf.setUrl("/media/products/DR001/1.webp");
            when(mediaFileRepo.findByProductIds(anyList())).thenReturn(List.of(mf));

            byte[] result = exportService.export("xml", "en", defaultFilter());
            String xml = new String(result);

            assertThat(xml).contains("<images>");
            assertThat(xml).contains("<image>https://pim.example.com/media/products/DR001/1.webp</image>");
        }

        @Test
        void export_xml_noAttributes_omitsDAndL() {
            testProduct.setAttributes(null);
            mockRepos(List.of(testProduct));

            byte[] result = exportService.export("xml", "en", defaultFilter());
            String xml = new String(result);

            assertThat(xml).doesNotContain("<d_mm>");
            assertThat(xml).doesNotContain("<l_mm>");
        }
    }

    // ========================= Default format =========================

    @Test
    @DisplayName("export — unknown format defaults to CSV")
    void export_unknownFormat_defaultsToCsv() {
        mockRepos(List.of(testProduct));

        byte[] result = exportService.export("unknown", "en", defaultFilter());
        String output = new String(result);

        assertThat(output).contains("\"tool_no\"");
    }
}
