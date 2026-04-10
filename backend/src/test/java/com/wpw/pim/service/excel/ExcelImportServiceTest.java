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
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.classifier.MachineClassifier;
import com.wpw.pim.service.excel.classifier.MaterialClassifier;
import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.RawGroupRow;
import com.wpw.pim.service.excel.dto.RawProductRow;
import com.wpw.pim.service.excel.dto.ValidationReport;
import com.wpw.pim.service.excel.parser.GroupsSheetParser;
import com.wpw.pim.service.excel.parser.ProductsSheetParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import com.wpw.pim.service.excel.validation.ImportValidator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link ExcelImportService}.
 * Тестирует валидацию и выполнение импорта с мок-зависимостями.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ExcelImportServiceTest {

    @Mock private ExcelImportProperties props;
    @Mock private ProductsSheetParser productsParser;
    @Mock private GroupsSheetParser groupsParser;
    @Mock private ImportValidator validator;
    @Mock private MaterialClassifier materialClassifier;
    @Mock private MachineClassifier machineClassifier;
    @Mock private CuttingTypeNormalizer cuttingTypeNormalizer;
    @Mock private ImportReportGenerator reportGenerator;
    @Mock private SectionRepository sectionRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private ProductGroupRepository groupRepo;
    @Mock private ProductRepository productRepo;
    @Mock private ProductTranslationRepository translationRepo;

    @InjectMocks
    private ExcelImportService excelImportService;

    private ExcelImportProperties.ProductsSheet productsSheet;
    private ExcelImportProperties.GroupsSheet groupsSheet;

    @BeforeEach
    void setUp() {
        productsSheet = new ExcelImportProperties.ProductsSheet();
        groupsSheet = new ExcelImportProperties.GroupsSheet();
    }

    // ========================= validate =========================

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        void validate_validFile_returnsReport() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(productsParser.parse(any(), any())).thenReturn(List.of());
            when(groupsParser.parse(any(), any())).thenReturn(List.of());
            when(productsParser.unknownHeaders(any())).thenReturn(List.of());

            ValidationReport expectedReport = ValidationReport.builder()
                .totalProductRows(0).totalGroupRows(0)
                .errorCount(0).warningCount(0).canProceed(true)
                .issues(List.of()).unknownHeaders(List.of())
                .build();
            when(validator.validate(any(), any(), any())).thenReturn(expectedReport);

            ValidationReport result = excelImportService.validate(file);

            assertThat(result.isCanProceed()).isTrue();
            assertThat(result.getErrorCount()).isZero();
            verify(productsParser).parse(any(), any());
            verify(groupsParser).parse(any(), any());
        }

        @Test
        void validate_missingProductsSheet_throwsException() throws Exception {
            MockMultipartFile file = createExcelFile("WrongName", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);

            assertThatThrownBy(() -> excelImportService.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void validate_missingGroupsSheet_throwsException() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "WrongName");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);

            assertThatThrownBy(() -> excelImportService.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product Groups");
        }
    }

    // ========================= execute =========================

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        void execute_validFile_returnsMarkdownReport() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);
            when(productsParser.parse(any(), any())).thenReturn(List.of());
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            // Section creation
            Section section = new Section();
            section.setSlug("wpw-tools");
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(reportGenerator.generate(any())).thenReturn("# Report");

            String result = excelImportService.execute(file);

            assertThat(result).isEqualTo("# Report");
            verify(reportGenerator).generate(any());
        }

        @Test
        void execute_missingSheets_throwsException() throws Exception {
            MockMultipartFile file = createExcelFile("WrongName", "WrongName2");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);

            assertThatThrownBy(() -> excelImportService.execute(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("нужных листов");
        }

        @Test
        void execute_withProducts_createsNewProducts() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawGroupRow groupRow = RawGroupRow.builder()
                .rowNum(3).groupId("GRP-001").groupCode("G001")
                .groupName("Router Bits").categoryName("Cutting Tools")
                .build();
            RawProductRow productRow = RawProductRow.builder()
                .rowNum(3).toolNo("DR001").groupId("GRP-001")
                .description("Diamond router bit").dMm("12.5").lMm("50")
                .materials("Carbide").machines("CNC Router")
                .build();

            when(productsParser.parse(any(), any())).thenReturn(List.of(productRow));
            when(groupsParser.parse(any(), any())).thenReturn(List.of(groupRow));

            Section section = new Section();
            section.setSlug("wpw-tools");
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            Category category = new Category();
            category.setSlug("cutting-tools");
            when(categoryRepo.findBySlug("cutting-tools")).thenReturn(Optional.of(category));

            ProductGroup group = new ProductGroup();
            group.setSlug("grp-001");
            when(groupRepo.findBySlug("grp-001")).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("DR001")).thenReturn(false);
            Product newProduct = new Product();
            newProduct.setId(java.util.UUID.randomUUID());
            newProduct.setToolNo("DR001");
            when(productRepo.findByToolNo("DR001")).thenReturn(Optional.empty());
            when(productRepo.save(any(Product.class))).thenReturn(newProduct);

            when(materialClassifier.classify(anyString())).thenReturn(
                new MaterialClassifier.Classification(
                    new java.util.LinkedHashSet<>(List.of("carbide")),
                    new java.util.LinkedHashSet<>()));
            when(machineClassifier.classify(anyString())).thenReturn(
                new MachineClassifier.Classification(
                    new java.util.LinkedHashSet<>(List.of("cnc_router")),
                    new java.util.LinkedHashSet<>()));

            when(reportGenerator.generate(any())).thenReturn("# Report\n1 created");

            String result = excelImportService.execute(file);

            assertThat(result).contains("Report");
            verify(productRepo).save(any(Product.class));
        }

        @Test
        void execute_productWithBlankToolNo_isSkipped() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawProductRow blankProduct = RawProductRow.builder()
                .rowNum(3).toolNo("").build();
            when(productsParser.parse(any(), any())).thenReturn(List.of(blankProduct));
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(productRepo, never()).save(any(Product.class));
        }

        @Test
        void execute_existingProduct_updates() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawProductRow productRow = RawProductRow.builder()
                .rowNum(3).toolNo("EXISTING-001").description("Updated")
                .materials("Wood").machines("CNC")
                .build();
            when(productsParser.parse(any(), any())).thenReturn(List.of(productRow));
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(productRepo.existsByToolNo("EXISTING-001")).thenReturn(true);
            Product existingProduct = new Product();
            existingProduct.setId(java.util.UUID.randomUUID());
            existingProduct.setToolNo("EXISTING-001");
            ProductAttributes attrs = new ProductAttributes();
            attrs.setProduct(existingProduct);
            existingProduct.setAttributes(attrs);
            when(productRepo.findByToolNo("EXISTING-001")).thenReturn(Optional.of(existingProduct));
            when(productRepo.save(any(Product.class))).thenReturn(existingProduct);

            when(materialClassifier.classify(anyString())).thenReturn(
                new MaterialClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(machineClassifier.classify(anyString())).thenReturn(
                new MachineClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report: 1 updated");

            String result = excelImportService.execute(file);

            assertThat(result).contains("updated");
            verify(productRepo).save(any(Product.class));
        }

        @Test
        void execute_newSection_createsSection() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            when(productsParser.parse(any(), any())).thenReturn(List.of());
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.empty());
            Section newSection = new Section();
            newSection.setId(java.util.UUID.randomUUID());
            when(sectionRepo.save(any(Section.class))).thenReturn(newSection);
            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(sectionRepo).save(any(Section.class));
        }

        @Test
        void execute_groupWithNullGroupId_isSkipped() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawGroupRow groupWithNull = RawGroupRow.builder()
                .rowNum(3).groupId(null).categoryName("Cat").build();
            when(productsParser.parse(any(), any())).thenReturn(List.of());
            when(groupsParser.parse(any(), any())).thenReturn(List.of(groupWithNull));

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(categoryRepo, never()).save(any(Category.class));
        }

        @Test
        void execute_productWithCatalogPage_parsesCatalogPage() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawProductRow productRow = RawProductRow.builder()
                .rowNum(3).toolNo("PAGE-001").catalogPage("42")
                .description("With page").materials("Wood").machines("CNC")
                .build();
            when(productsParser.parse(any(), any())).thenReturn(List.of(productRow));
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(productRepo.existsByToolNo("PAGE-001")).thenReturn(false);
            when(productRepo.findByToolNo("PAGE-001")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(java.util.UUID.randomUUID());
            saved.setToolNo("PAGE-001");
            when(productRepo.save(any(Product.class))).thenReturn(saved);

            when(materialClassifier.classify(anyString())).thenReturn(
                new MaterialClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(machineClassifier.classify(anyString())).thenReturn(
                new MachineClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(productRepo).save(argThat(p -> ((Product) p).getCatalogPage() != null
                    && ((Product) p).getCatalogPage() == 42));
        }

        @Test
        void execute_productWithBallBearingAndRetainer_setsFlags() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawProductRow productRow = RawProductRow.builder()
                .rowNum(3).toolNo("BEARING-001")
                .description("With bearing")
                .ballBearing("BB-001").retainer("RET-001")
                .materials("Wood").machines("CNC")
                .build();
            when(productsParser.parse(any(), any())).thenReturn(List.of(productRow));
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(productRepo.existsByToolNo("BEARING-001")).thenReturn(false);
            when(productRepo.findByToolNo("BEARING-001")).thenReturn(Optional.empty());
            Product saved = new Product();
            saved.setId(java.util.UUID.randomUUID());
            saved.setToolNo("BEARING-001");
            when(productRepo.save(any(Product.class))).thenReturn(saved);

            when(materialClassifier.classify(anyString())).thenReturn(
                new MaterialClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(machineClassifier.classify(anyString())).thenReturn(
                new MachineClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(productRepo).save(any(Product.class));
        }

        @Test
        void execute_productThrowsException_accumulatesError() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawProductRow productRow = RawProductRow.builder()
                .rowNum(3).toolNo("ERR-001").description("Will fail")
                .materials("Wood").machines("CNC")
                .build();
            when(productsParser.parse(any(), any())).thenReturn(List.of(productRow));
            when(groupsParser.parse(any(), any())).thenReturn(List.of());

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(productRepo.existsByToolNo("ERR-001")).thenReturn(false);
            when(productRepo.findByToolNo("ERR-001")).thenReturn(Optional.empty());
            when(productRepo.save(any(Product.class))).thenThrow(new RuntimeException("DB error"));

            when(materialClassifier.classify(any())).thenReturn(
                new MaterialClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));
            when(machineClassifier.classify(any())).thenReturn(
                new MachineClassifier.Classification(
                    new java.util.LinkedHashSet<>(), new java.util.LinkedHashSet<>()));

            when(reportGenerator.generate(any())).thenReturn("# Report with errors");

            String result = excelImportService.execute(file);

            assertThat(result).contains("errors");
        }

        @Test
        void execute_newCategoryAndGroup_createsAll() throws Exception {
            MockMultipartFile file = createExcelFile("Products", "Product Groups");

            when(props.getProductsSheet()).thenReturn(productsSheet);
            when(props.getGroupsSheet()).thenReturn(groupsSheet);
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);

            RawGroupRow groupRow = RawGroupRow.builder()
                .rowNum(3).groupId("NEW-GRP").groupCode("NG01")
                .groupName("New Group").categoryName("New Category")
                .build();
            when(productsParser.parse(any(), any())).thenReturn(List.of());
            when(groupsParser.parse(any(), any())).thenReturn(List.of(groupRow));

            Section section = new Section();
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(categoryRepo.findBySlug("new-category")).thenReturn(Optional.empty());
            Category newCat = new Category();
            newCat.setId(java.util.UUID.randomUUID());
            when(categoryRepo.save(any(Category.class))).thenReturn(newCat);

            when(groupRepo.findBySlug("new-grp")).thenReturn(Optional.empty());
            ProductGroup newGroup = new ProductGroup();
            newGroup.setId(java.util.UUID.randomUUID());
            when(groupRepo.save(any(ProductGroup.class))).thenReturn(newGroup);

            when(reportGenerator.generate(any())).thenReturn("# Report");

            excelImportService.execute(file);

            verify(categoryRepo).save(any(Category.class));
            verify(groupRepo).save(any(ProductGroup.class));
        }
    }

    // ========================= helpers =========================

    private MockMultipartFile createExcelFile(String productsSheetName, String groupsSheetName)
            throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet ps = wb.createSheet(productsSheetName);
            Row header = ps.createRow(1); // headerRow=2 -> idx=1
            header.createCell(0).setCellValue("Tool No");

            Sheet gs = wb.createSheet(groupsSheetName);
            Row gHeader = gs.createRow(1);
            gHeader.createCell(0).setCellValue("Group ID");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", "catalog.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray());
        }
    }
}
