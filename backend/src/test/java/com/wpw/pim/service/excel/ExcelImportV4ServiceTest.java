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
}
