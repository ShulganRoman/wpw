package com.wpw.pim.service.excel;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.service.excel.config.ExcelImportV4Properties;
import com.wpw.pim.service.excel.dto.RawV4Row;
import com.wpw.pim.service.excel.dto.ValidationReport;
import com.wpw.pim.service.excel.parser.V4SheetParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import com.wpw.pim.service.excel.validation.ImportV4Validator;
import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * Unit-тесты для {@link ExcelImportV4Service}.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ExcelImportV4ServiceTest {

    @Mock private ExcelImportV4Properties props;
    @Mock private V4SheetParser parser;
    @Mock private ImportV4Validator validator;
    @Mock private CuttingTypeNormalizer cuttingTypeNormalizer;
    @Mock private ImportReportGenerator reportGenerator;
    @Mock private SectionRepository sectionRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private ProductGroupRepository groupRepo;
    @Mock private ProductRepository productRepo;
    @Mock private ProductTranslationRepository translationRepo;

    @InjectMocks
    private ExcelImportV4Service service;

    private MockMultipartFile createExcelFile(String sheetName) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet(sheetName);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
        }
    }

    // ========================= validate =========================

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("missing sheet throws IllegalArgumentException")
        void validate_missingSheet_throws() throws Exception {
            MockMultipartFile file = createExcelFile("WrongName");
            when(props.getSheetName()).thenReturn("Products");

            assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Products");
        }

        @Test
        @DisplayName("valid file returns ValidationReport")
        void validate_validFile_returnsReport() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            when(props.getSheetName()).thenReturn("Products");
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);
            when(props.getColumns()).thenReturn(new ExcelImportV4Properties.Columns());

            when(parser.parse(any(Sheet.class), any())).thenReturn(Collections.emptyList());
            when(parser.unknownHeaders(any(Sheet.class))).thenReturn(Collections.emptyList());

            ValidationReport expectedReport = ValidationReport.builder()
                .totalProductRows(0).totalGroupRows(0)
                .errorCount(0).warningCount(0).canProceed(true)
                .issues(Collections.emptyList()).unknownHeaders(Collections.emptyList())
                .build();
            when(validator.validate(any(), any())).thenReturn(expectedReport);

            ValidationReport result = service.validate(file);

            assertThat(result.isCanProceed()).isTrue();
            assertThat(result.getErrorCount()).isZero();
            verify(parser).parse(any(Sheet.class), any());
        }
    }

    // ========================= helper to mock catalog defaults =========================

    private void stubDefaultCatalog() {
        Section section = new Section();
        section.setSlug("wpw-tools");
        section.setId(java.util.UUID.randomUUID());
        when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));
    }

    private Product stubNewProductSave(String toolNo) {
        Product newProduct = new Product();
        newProduct.setId(java.util.UUID.randomUUID());
        newProduct.setToolNo(toolNo);
        when(productRepo.existsByToolNo(toolNo)).thenReturn(false);
        when(productRepo.findByToolNo(toolNo)).thenReturn(Optional.empty());
        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId(newProduct.getId());
            return p;
        });
        return newProduct;
    }

    private void stubMissingFile() throws Exception {
        when(props.getSheetName()).thenReturn("Products");
        when(props.getHeaderRow()).thenReturn(2);
        when(props.getDataStartRow()).thenReturn(3);
        when(props.getColumns()).thenReturn(new ExcelImportV4Properties.Columns());
        when(reportGenerator.generate(any())).thenReturn("# Report");
    }

    // ========================= execute =========================

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("new product - productRepo.save() is called")
        void execute_newProduct_savesCalled() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            when(props.getSheetName()).thenReturn("Products");
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);
            when(props.getColumns()).thenReturn(new ExcelImportV4Properties.Columns());

            RawV4Row row = RawV4Row.builder()
                .rowNum(3).toolNo("DT12702").name("Test Bit")
                .categoryName("Router Bits").groupName("Spiral Brazed Bits")
                .status("active").productType("main")
                .build();

            when(parser.parse(any(Sheet.class), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setSlug("wpw-tools");
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            Category category = new Category();
            category.setSlug("router-bits");
            when(categoryRepo.findBySlug("router-bits")).thenReturn(Optional.of(category));

            ProductGroup group = new ProductGroup();
            group.setSlug("spiral-brazed-bits");
            when(groupRepo.findBySlug("spiral-brazed-bits")).thenReturn(Optional.of(group));

            when(productRepo.existsByToolNo("DT12702")).thenReturn(false);
            Product newProduct = new Product();
            newProduct.setId(java.util.UUID.randomUUID());
            newProduct.setToolNo("DT12702");
            when(productRepo.findByToolNo("DT12702")).thenReturn(Optional.empty());
            when(productRepo.save(any(Product.class))).thenReturn(newProduct);

            when(reportGenerator.generate(any())).thenReturn("# Report");

            String result = service.execute(file);

            assertThat(result).isEqualTo("# Report");
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("row without toolNo - save() not called")
        void execute_noToolNo_saveNotCalled() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            when(props.getSheetName()).thenReturn("Products");
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);
            when(props.getColumns()).thenReturn(new ExcelImportV4Properties.Columns());

            // Parser returns rows without toolNo (which shouldn't happen normally since
            // parser skips them, but service also checks)
            RawV4Row row = RawV4Row.builder().rowNum(3).build();
            when(parser.parse(any(Sheet.class), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setSlug("wpw-tools");
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(file);

            verify(productRepo, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("creates group from categoryName + groupName")
        void execute_createsGroupFromCategoryAndGroupName() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            when(props.getSheetName()).thenReturn("Products");
            when(props.getHeaderRow()).thenReturn(2);
            when(props.getDataStartRow()).thenReturn(3);
            when(props.getColumns()).thenReturn(new ExcelImportV4Properties.Columns());

            RawV4Row row = RawV4Row.builder()
                .rowNum(3).toolNo("TEST-001").name("Test")
                .categoryName("Router Bits").groupName("Flush Trim Bits")
                .build();

            when(parser.parse(any(Sheet.class), any())).thenReturn(List.of(row));

            Section section = new Section();
            section.setSlug("wpw-tools");
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.of(section));

            // Category not found → should be created
            when(categoryRepo.findBySlug("router-bits")).thenReturn(Optional.empty());
            Category newCat = new Category();
            newCat.setSlug("router-bits");
            when(categoryRepo.save(any(Category.class))).thenReturn(newCat);

            // Group not found → should be created
            when(groupRepo.findBySlug("flush-trim-bits")).thenReturn(Optional.empty());
            ProductGroup newGroup = new ProductGroup();
            newGroup.setSlug("flush-trim-bits");
            when(groupRepo.save(any(ProductGroup.class))).thenReturn(newGroup);

            when(productRepo.existsByToolNo("TEST-001")).thenReturn(false);
            Product newProduct = new Product();
            newProduct.setId(java.util.UUID.randomUUID());
            newProduct.setToolNo("TEST-001");
            when(productRepo.findByToolNo("TEST-001")).thenReturn(Optional.empty());
            when(productRepo.save(any(Product.class))).thenReturn(newProduct);

            when(reportGenerator.generate(any())).thenReturn("# Report");

            service.execute(file);

            // Verify category and group were created
            verify(categoryRepo).save(any(Category.class));
            verify(groupRepo).save(any(ProductGroup.class));
        }
    }

    // ========================= execute — sheet missing =========================

    @Nested
    @DisplayName("execute — sheet missing")
    class ExecuteSheetMissing {

        @Test
        @DisplayName("missing sheet during execute throws IllegalArgumentException")
        void execute_missingSheet_throws() throws Exception {
            MockMultipartFile file = createExcelFile("WrongName");
            when(props.getSheetName()).thenReturn("Products");

            assertThatThrownBy(() -> service.execute(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Products");
        }
    }

    // ========================= execute — catalog cache reuse =========================

    @Nested
    @DisplayName("execute — catalog: reuse existing")
    class ExecuteCatalogReuse {

        @Test
        @DisplayName("two rows with same category/group reuse caches")
        void execute_sameCategoryAndGroup_reusesCache() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            stubMissingFile();

            RawV4Row r1 = RawV4Row.builder().rowNum(3).toolNo("A1").categoryName("Cat1").groupName("G1").build();
            RawV4Row r2 = RawV4Row.builder().rowNum(4).toolNo("A2").categoryName("Cat1").groupName("G1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r1, r2));

            stubDefaultCatalog();
            when(categoryRepo.findBySlug("cat1")).thenReturn(Optional.empty());
            when(categoryRepo.findByNameEnIgnoreCase("Cat1")).thenReturn(Optional.empty());
            Category cat = new Category();
            cat.setId(java.util.UUID.randomUUID());
            cat.setSlug("cat1");
            cat.setTranslations(new java.util.HashMap<>(java.util.Map.of("en", "Cat1")));
            when(categoryRepo.save(any())).thenReturn(cat);

            when(groupRepo.findByCategoryIdAndSlug(cat.getId(), "g1")).thenReturn(Optional.empty());
            when(groupRepo.findByCategoryIdAndNameEnIgnoreCase(cat.getId(), "G1")).thenReturn(Optional.empty());
            when(groupRepo.findBySlug("g1")).thenReturn(Optional.empty());
            ProductGroup pg = new ProductGroup();
            pg.setId(java.util.UUID.randomUUID());
            pg.setSlug("g1");
            when(groupRepo.save(any())).thenReturn(pg);

            stubNewProductSave("A1");
            when(productRepo.existsByToolNo("A2")).thenReturn(false);
            when(productRepo.findByToolNo("A2")).thenReturn(Optional.empty());

            service.execute(file);

            // Создаются по одному разу (несмотря на 2 строки)
            verify(categoryRepo, times(1)).save(any(Category.class));
            verify(groupRepo, times(1)).save(any(ProductGroup.class));
        }

        @Test
        @DisplayName("group: slug taken globally — adds category prefix")
        void execute_groupSlugTakenGlobally_addsCategoryPrefix() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            stubMissingFile();

            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").categoryName("Cat1").groupName("Mills").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));

            stubDefaultCatalog();
            Category cat = new Category();
            cat.setId(java.util.UUID.randomUUID());
            cat.setSlug("cat1");
            cat.setTranslations(new java.util.HashMap<>(java.util.Map.of("en", "Cat 1")));
            when(categoryRepo.findBySlug("cat1")).thenReturn(Optional.of(cat));
            // Это группа из другой категории с тем же slug
            when(groupRepo.findByCategoryIdAndSlug(cat.getId(), "mills")).thenReturn(Optional.empty());
            when(groupRepo.findByCategoryIdAndNameEnIgnoreCase(cat.getId(), "Mills")).thenReturn(Optional.empty());
            ProductGroup existingOther = new ProductGroup();
            existingOther.setId(java.util.UUID.randomUUID());
            existingOther.setSlug("mills");
            when(groupRepo.findBySlug("mills")).thenReturn(Optional.of(existingOther));
            when(groupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<ProductGroup> captor =
                org.mockito.ArgumentCaptor.forClass(ProductGroup.class);
            verify(groupRepo).save(captor.capture());
            // Slug должен иметь префикс категории
            assertThat(captor.getValue().getSlug()).isEqualTo("cat-1-mills");
        }

        @Test
        @DisplayName("section creation when missing")
        void execute_sectionCreatedIfMissing() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            stubMissingFile();

            when(parser.parse(any(), any())).thenReturn(List.of());
            when(sectionRepo.findBySlug("wpw-tools")).thenReturn(Optional.empty());
            Section savedSection = new Section();
            savedSection.setId(java.util.UUID.randomUUID());
            savedSection.setSlug("wpw-tools");
            when(sectionRepo.save(any())).thenReturn(savedSection);

            service.execute(file);

            verify(sectionRepo).save(any(Section.class));
        }

        @Test
        @DisplayName("category found by name fallback")
        void execute_categoryFoundByNameFallback() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            stubMissingFile();

            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").categoryName("Cat1").groupName("G1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));

            stubDefaultCatalog();
            // findBySlug → Optional.empty(); findByNameEnIgnoreCase → present
            Category existing = new Category();
            existing.setId(java.util.UUID.randomUUID());
            existing.setSlug("cat1");
            existing.setTranslations(new java.util.HashMap<>(java.util.Map.of("en", "Cat1")));
            when(categoryRepo.findBySlug("cat1")).thenReturn(Optional.empty());
            when(categoryRepo.findByNameEnIgnoreCase("Cat1")).thenReturn(Optional.of(existing));

            ProductGroup pg = new ProductGroup();
            pg.setId(java.util.UUID.randomUUID());
            pg.setSlug("g1");
            when(groupRepo.findByCategoryIdAndSlug(existing.getId(), "g1")).thenReturn(Optional.of(pg));

            stubNewProductSave("A1");

            service.execute(file);

            // Category не создаётся (найдена по имени)
            verify(categoryRepo, never()).save(any(Category.class));
            verify(groupRepo, never()).save(any(ProductGroup.class));
        }

        @Test
        @DisplayName("catalog error during build is recorded but doesn't abort")
        void execute_catalogError_recordedAsError() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            stubMissingFile();

            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").categoryName("Cat1").groupName("G1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));

            stubDefaultCatalog();
            when(categoryRepo.findBySlug("cat1")).thenThrow(new RuntimeException("DB error"));

            stubNewProductSave("A1");

            // Импорт не должен упасть — продукт сохраняется (без группы)
            service.execute(file);

            verify(productRepo).save(any(Product.class));
        }
    }

    // ========================= execute — product attributes =========================

    @Nested
    @DisplayName("execute — product attributes")
    class ExecuteAttributes {

        @org.junit.jupiter.api.BeforeEach
        void setUp() throws Exception {
            stubMissingFile();
            stubDefaultCatalog();
        }

        @Test
        @DisplayName("status invalid value preserves default")
        void status_invalidValue_keepsDefault() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").status("BAD").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
            // Default ProductStatus = active (т.к. поле не было установлено через invalid value)
            // Теперь ProductStatus.active — значение по умолчанию
            assertThat(cap.getValue().getStatus()).isEqualTo(com.wpw.pim.domain.enums.ProductStatus.active);
        }

        @Test
        @DisplayName("status null sets default active")
        void status_null_setsDefault() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(com.wpw.pim.domain.enums.ProductStatus.active);
        }

        @Test
        @DisplayName("orderable=No sets false")
        void orderable_no_setsFalse() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").orderable("no").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
            assertThat(cap.getValue().isOrderable()).isFalse();
        }

        @Test
        @DisplayName("productType invalid preserves default")
        void productType_invalid_preservesDefault() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").productType("invalid").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
        }

        @Test
        @DisplayName("catalogPage valid number sets short value")
        void catalogPage_validNumber_set() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").catalogPage("42").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
            assertThat(cap.getValue().getCatalogPage()).isEqualTo((short) 42);
        }

        @Test
        @DisplayName("catalogPage non-numeric ignored")
        void catalogPage_nonNumeric_ignored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").catalogPage("abc").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("application/material/machine tags split into sets")
        void tagsAndMaterials_split() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .applicationTags("cut, drill ,plane")
                .toolMaterials("HSS,carbide")
                .workpieceMaterials("wood,plastic")
                .machineTypes("router")
                .machineBrands("dewalt,bosch")
                .build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            org.mockito.ArgumentCaptor<Product> cap = org.mockito.ArgumentCaptor.forClass(Product.class);
            verify(productRepo).save(cap.capture());
            Product saved = cap.getValue();
            assertThat(saved.getOperationCodes()).contains("cut", "drill", "plane");
            assertThat(saved.getToolMaterials()).contains("HSS", "carbide");
            assertThat(saved.getMachineTypes()).contains("router");
        }

        @Test
        @DisplayName("attributes: dimensions parsed correctly")
        void attributes_dimensions() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .dMm("12.7").d1Mm("8").d2Mm("3").bMm("25").b1Mm("5")
                .lMm("75").l1Mm("30").rMm("0.5").aMm("2.5")
                .angleDeg("60").shankMm("8").shankInch("1/4")
                .flutes("4").bladeNo("2")
                .weightG("100").pkgQty("10").cartonQty("100").stockQty("500")
                .ean13("1234567890123").upc12("123456789012").hsCode("8207")
                .countryOfOrigin("CN")
                .build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("attributes: invalid numeric values ignored")
        void attributes_invalidNumeric_ignored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .dMm("not-a-number").flutes("xx").weightG("kg").pkgQty("box")
                .build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("rotationDirection invalid value ignored")
        void rotationDirection_invalid_ignored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").rotationDirection("invalid").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("boreType invalid value ignored")
        void boreType_invalid_ignored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").boreType("invalid").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("ballBearing only — sets hasBallBearing=true and code")
        void ballBearing_codeOnly_setsHasBallBearing() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").ballBearing("BB-1234").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("hasBallBearing yes + ballBearing code — both set")
        void hasBallBearing_andCode_bothSet() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .hasBallBearing("yes").ballBearing("BB-9999").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("retainer code only — sets hasRetainer=true")
        void retainer_codeOnly_setsHasRetainer() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").retainer("RT-1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("hasRetainer + retainer code — both set")
        void hasRetainer_andCode_bothSet() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .hasRetainer("yes").retainer("RT-7").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("stockStatus valid value")
        void stockStatus_valid_set() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").stockStatus("in_stock").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("stockStatus invalid value ignored")
        void stockStatus_invalid_ignored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").stockStatus("WHATEVER").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("cuttingType normalized via normalizer")
        void cuttingType_normalized() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").cuttingType("Up").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            when(cuttingTypeNormalizer.normalize("Up")).thenReturn("up");

            service.execute(file);
            verify(cuttingTypeNormalizer).normalize("Up");
        }

        @Test
        @DisplayName("canResharpen with truthy value")
        void canResharpen_truthy_set() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").canResharpen("Y").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }
    }

    // ========================= execute — translation upsert =========================

    @Nested
    @DisplayName("execute — translations")
    class ExecuteTranslation {

        @org.junit.jupiter.api.BeforeEach
        void setUp() throws Exception {
            stubMissingFile();
            stubDefaultCatalog();
        }

        @Test
        @DisplayName("translation skipped when no name/description")
        void translation_noData_notSaved() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");

            service.execute(file);

            verify(translationRepo, never()).save(any());
        }

        @Test
        @DisplayName("translation: name explicit — uses provided name")
        void translation_nameExplicit() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").name("My Tool").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(file);
            verify(translationRepo).save(any());
        }

        @Test
        @DisplayName("translation: fallback name from toolNo + groupName")
        void translation_fallbackName_fromToolNoAndGroup() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1")
                .shortDescription("desc")
                .groupName("Mills")
                .build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(file);
            // groupName тоже задаст cat:group маршрут, но без category — игнор
            verify(translationRepo).save(any());
        }

        @Test
        @DisplayName("translation: fallback name when only longDescription")
        void translation_fallbackName_onlyLongDesc() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").longDescription("long").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(file);
            verify(translationRepo).save(any());
        }

        @Test
        @DisplayName("translation: applicationTags non-blank stored")
        void translation_applicationTags_stored() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder()
                .rowNum(3).toolNo("A1").name("Tool").applicationTags("cut,drill").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            stubNewProductSave("A1");
            when(translationRepo.findById(any())).thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(file);
            verify(translationRepo).save(any());
        }

        @Test
        @DisplayName("existing product is updated, not created")
        void update_existingProduct() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));

            Product existing = new Product();
            existing.setId(java.util.UUID.randomUUID());
            existing.setToolNo("A1");
            when(productRepo.existsByToolNo("A1")).thenReturn(true);
            when(productRepo.findByToolNo("A1")).thenReturn(Optional.of(existing));
            when(productRepo.save(any())).thenReturn(existing);

            service.execute(file);
            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("rare error in importProduct is recorded, processing continues")
        void importProduct_error_recorded() throws Exception {
            MockMultipartFile file = createExcelFile("Products");
            RawV4Row r = RawV4Row.builder().rowNum(3).toolNo("A1").build();
            when(parser.parse(any(), any())).thenReturn(List.of(r));
            when(productRepo.existsByToolNo("A1")).thenReturn(false);
            when(productRepo.findByToolNo("A1")).thenReturn(Optional.empty());
            when(productRepo.save(any())).thenThrow(new RuntimeException("DB fail"));

            String result = service.execute(file);
            assertThat(result).isEqualTo("# Report");
        }
    }
}
