package com.wpw.pim.service.excel;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.repository.operation.OperationRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.dto.ValidationReport;
import com.wpw.pim.service.excel.dto.WpwCatalogRow;
import com.wpw.pim.service.excel.parser.WpwCatalogParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
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

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WpwCatalogImportServiceTest {

    @Mock private WpwCatalogParser parser;
    @Mock private CuttingTypeNormalizer cuttingTypeNormalizer;
    @Mock private ImportReportGenerator reportGenerator;
    @Mock private SectionRepository sectionRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private ProductGroupRepository groupRepo;
    @Mock private ProductRepository productRepo;
    @Mock private ProductTranslationRepository translationRepo;
    @Mock private OperationRepository operationRepo;

    @InjectMocks private WpwCatalogImportService service;

    private MockMultipartFile createWpwExcelFile() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            String[] headers = {"SKU", "Product_Type", "Category", "Group", "Description_EN",
                    "Name_RU", "AI_Description_EN", "D_mm", "D1_mm", "B_mm", "L_mm", "R_mm",
                    "Angle_deg", "Shank_mm", "Shank_inch", "Flutes", "Tool_Material",
                    "Cutting_Type", "Application_Tags", "Workpiece_Material", "Machine_Type"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("WPW-001");
            data.createCell(1).setCellValue("main");
            data.createCell(2).setCellValue("Router Bits");
            data.createCell(3).setCellValue("Straight Bits");
            data.createCell(4).setCellValue("Straight router bit");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", "WPW_Catalog_v3.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }

    private MockMultipartFile createExcelWithoutSheet() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("WrongSheet");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", "wrong.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("valid file returns report with canProceed=true")
        void validFile_returnsOkReport() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test bit").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            MockMultipartFile file = createWpwExcelFile();
            ValidationReport report = service.validate(file);

            assertThat(report.getTotalProductRows()).isEqualTo(1);
            assertThat(report.isCanProceed()).isTrue();
            assertThat(report.getErrorCount()).isZero();
        }

        @Test
        @DisplayName("missing SKU produces error")
        void missingSku_producesError() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku(null).category("Bits").group("Straight").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getErrorCount()).isEqualTo(1);
            assertThat(report.isCanProceed()).isFalse();
        }

        @Test
        @DisplayName("missing category produces error")
        void missingCategory_producesError() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category(null).group("Straight")
                    .descriptionEn("test").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getErrorCount()).isEqualTo(1);
            assertThat(report.isCanProceed()).isFalse();
        }

        @Test
        @DisplayName("duplicate SKU produces warning")
        void duplicateSku_producesWarning() throws Exception {
            WpwCatalogRow r1 = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test").build();
            WpwCatalogRow r2 = WpwCatalogRow.builder()
                    .rowNum(3).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test 2").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r1, r2));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("non-numeric D_mm produces warning")
        void nonNumericDMm_producesWarning() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test").dMm("abc").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
            assertThat(report.isCanProceed()).isTrue();
        }

        @Test
        @DisplayName("missing Sheet1 throws IllegalArgumentException")
        void missingSheet_throwsException() throws Exception {
            assertThatThrownBy(() -> service.validate(createExcelWithoutSheet()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sheet1");
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("imports products and returns report")
        void execute_importsProducts() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").productType("main")
                    .category("Router Bits").group("Straight Bits")
                    .descriptionEn("Straight router bit").dMm("12.5").shankMm("8")
                    .build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));

            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-001")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-001")).thenReturn(Optional.empty());
            Product savedProduct = new Product();
            savedProduct.setId(UUID.randomUUID());
            savedProduct.setToolNo("WPW-001");
            when(productRepo.save(any(Product.class))).thenReturn(savedProduct);

            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            when(reportGenerator.generate(any())).thenReturn("# Import Report");

            String report = service.execute(createWpwExcelFile());

            assertThat(report).isEqualTo("# Import Report");
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("skips rows with blank required fields")
        void execute_skipsBlankRows() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category(null).group("Straight").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            String report = service.execute(createWpwExcelFile());

            assertThat(report).isNotNull();
            verify(productRepo, never()).save(any());
        }

        @Test
        @DisplayName("missing Sheet1 in execute throws exception")
        void execute_missingSheet_throws() {
            assertThatThrownBy(() -> service.execute(createExcelWithoutSheet()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sheet1");
        }

        @Test
        @DisplayName("imports existing product (update)")
        void execute_existingProduct_updates() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-EXIST").productType("main")
                    .category("Router Bits").group("Straight Bits")
                    .descriptionEn("Updated description").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));

            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-EXIST")).thenReturn(true);
            Product existingProduct = new Product();
            existingProduct.setId(UUID.randomUUID());
            existingProduct.setToolNo("WPW-EXIST");
            ProductAttributes attrs = new ProductAttributes();
            attrs.setProduct(existingProduct);
            existingProduct.setAttributes(attrs);
            when(productRepo.findByToolNo("WPW-EXIST")).thenReturn(Optional.of(existingProduct));
            when(productRepo.save(any(Product.class))).thenReturn(existingProduct);

            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report: 1 updated");

            String report = service.execute(createWpwExcelFile());

            assertThat(report).contains("updated");
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("creates new section if not found")
        void execute_noSection_createsNew() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-NEW-SEC").productType("main")
                    .category("New Cat").group("New Group")
                    .descriptionEn("Test").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.empty());
            Section newSection = new Section();
            newSection.setId(UUID.randomUUID());
            when(sectionRepo.save(any(Section.class))).thenReturn(newSection);

            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));

            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-NEW-SEC")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-NEW-SEC")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(UUID.randomUUID());
            saved.setToolNo("WPW-NEW-SEC");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(createWpwExcelFile());

            verify(sectionRepo).save(any(Section.class));
        }

        @Test
        @DisplayName("creates new category and group if not found")
        void execute_newCategoryAndGroup_createsAll() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-NEWCAT").productType("spare_part")
                    .category("New Category").group("New Group")
                    .descriptionEn("Test").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.empty());
            Category newCat = new Category();
            newCat.setId(UUID.randomUUID());
            when(categoryRepo.save(any(Category.class))).thenReturn(newCat);

            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.empty());
            ProductGroup newGroup = new ProductGroup();
            newGroup.setId(UUID.randomUUID());
            when(groupRepo.save(any(ProductGroup.class))).thenReturn(newGroup);

            when(productRepo.existsByToolNo("WPW-NEWCAT")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-NEWCAT")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(UUID.randomUUID());
            saved.setToolNo("WPW-NEWCAT");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(createWpwExcelFile());

            verify(categoryRepo).save(any(Category.class));
            verify(groupRepo).save(any(ProductGroup.class));
        }

        @Test
        @DisplayName("product with application tags creates operations")
        void execute_withApplicationTags_createsOperations() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-TAG").productType("main")
                    .category("Bits").group("Straight")
                    .descriptionEn("Test").applicationTags("Cove Cutting, Grooving").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));
            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-TAG")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-TAG")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(UUID.randomUUID());
            saved.setToolNo("WPW-TAG");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(operationRepo.existsById(anyString())).thenReturn(false);
            when(operationRepo.count()).thenReturn(0L);
            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(createWpwExcelFile());

            verify(operationRepo, atLeast(2)).save(any());
        }

        @Test
        @DisplayName("product with Russian name creates RU translation")
        void execute_withRussianName_createsRuTranslation() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-RU").productType("main")
                    .category("Bits").group("Straight")
                    .descriptionEn("Test").nameRu("Тестовая фреза").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));
            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-RU")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-RU")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(UUID.randomUUID());
            saved.setToolNo("WPW-RU");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(createWpwExcelFile());

            // EN + RU = 2 translations saved
            verify(translationRepo, times(2)).save(any());
        }

        @Test
        @DisplayName("exception during product import is caught and accumulated")
        void execute_productThrowsException_accumulatesError() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-ERR").productType("main")
                    .category("Bits").group("Straight")
                    .descriptionEn("Test").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));
            when(groupRepo.findBySlug(anyString())).thenThrow(new RuntimeException("DB error"));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            String report = service.execute(createWpwExcelFile());

            assertThat(report).isNotNull();
        }

        @Test
        @DisplayName("product with all numeric attributes parses correctly")
        void execute_numericAttributes_parsedCorrectly() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-NUM").productType("main")
                    .category("Bits").group("Straight").descriptionEn("Test")
                    .dMm("12.5").d1Mm("10.0").bMm("3.0").lMm("50")
                    .rMm("1.5").angleDeg("45").shankMm("8").shankInch("5/16")
                    .flutes("2").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setId(UUID.randomUUID());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            Category category = new Category();
            category.setId(UUID.randomUUID());
            when(categoryRepo.findBySlug(anyString())).thenReturn(Optional.of(category));
            ProductGroup group = new ProductGroup();
            group.setId(UUID.randomUUID());
            when(groupRepo.findBySlug(anyString())).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("WPW-NUM")).thenReturn(false);
            when(productRepo.findByToolNo("WPW-NUM")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(UUID.randomUUID());
            saved.setToolNo("WPW-NUM");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(createWpwExcelFile());

            verify(productRepo).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("validate edge cases")
    class ValidateEdgeCases {

        @Test
        @DisplayName("missing group produces error")
        void missingGroup_producesError() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group(null)
                    .descriptionEn("Test").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getErrorCount()).isEqualTo(1);
            assertThat(report.isCanProceed()).isFalse();
        }

        @Test
        @DisplayName("missing description produces warning")
        void missingDescription_producesWarning() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn(null).build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
            assertThat(report.isCanProceed()).isTrue();
        }

        @Test
        @DisplayName("range value like 3-5 produces warning")
        void rangeValue_producesWarning() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test").dMm("3-5").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("non-numeric flutes produces warning")
        void nonNumericFlutes_producesWarning() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test").flutes("abc").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("integer flutes as double (2.0) does not produce warning")
        void flutesAsDouble_noWarning() throws Exception {
            WpwCatalogRow row = WpwCatalogRow.builder()
                    .rowNum(2).sku("WPW-001").category("Bits").group("Straight")
                    .descriptionEn("Test").flutes("2.0").build();
            when(parser.parse(any(), any())).thenReturn(List.of(row));
            when(parser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport report = service.validate(createWpwExcelFile());

            assertThat(report.isCanProceed()).isTrue();
            // No flutes warning for "2.0" since it's a whole number
        }
    }
}
