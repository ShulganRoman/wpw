package com.wpw.pim.service.excel;

import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.dto.RawGroupRow;
import com.wpw.pim.service.excel.dto.RawProductRow;
import com.wpw.pim.service.excel.dto.ValidationIssue;
import com.wpw.pim.service.excel.dto.ValidationReport;
import com.wpw.pim.service.excel.validation.ImportValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ImportValidator}.
 * Проверяют валидацию групп и продуктов перед импортом.
 */
@ExtendWith(MockitoExtension.class)
class ImportValidatorTest {

    @Mock
    private CuttingTypeNormalizer cuttingTypeNormalizer;

    private ImportValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImportValidator(cuttingTypeNormalizer);
    }

    // ========================= Пустые данные =========================

    @Test
    @DisplayName("validate — пустые списки — без ошибок, можно продолжать")
    void validate_emptyLists_canProceed() {
        ValidationReport report = validator.validate(List.of(), List.of(), List.of());

        assertThat(report.isCanProceed()).isTrue();
        assertThat(report.getErrorCount()).isZero();
        assertThat(report.getWarningCount()).isZero();
        assertThat(report.getTotalProductRows()).isZero();
        assertThat(report.getTotalGroupRows()).isZero();
    }

    // ========================= Groups validation =========================

    @Nested
    @DisplayName("validateGroups")
    class ValidateGroups {

        @Test
        void validate_duplicateGroupId_warningIssue() {
            RawGroupRow g1 = RawGroupRow.builder().rowNum(3).groupId("GRP-001")
                .groupName("Name1").categoryName("Cat1").build();
            RawGroupRow g2 = RawGroupRow.builder().rowNum(4).groupId("GRP-001")
                .groupName("Name2").categoryName("Cat2").build();

            ValidationReport report = validator.validate(List.of(), List.of(g1, g2), List.of());

            assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("groupId")
                    && i.getMessage().contains("Дублирующийся"));
        }

        @Test
        void validate_blankGroupName_warningIssue() {
            RawGroupRow g = RawGroupRow.builder().rowNum(3).groupId("GRP-001")
                .groupName(null).categoryName("Cat1").build();

            ValidationReport report = validator.validate(List.of(), List.of(g), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("groupName"));
        }

        @Test
        void validate_blankCategory_errorIssue() {
            RawGroupRow g = RawGroupRow.builder().rowNum(3).groupId("GRP-001")
                .groupName("Name").categoryName(null).build();

            ValidationReport report = validator.validate(List.of(), List.of(g), List.of());

            assertThat(report.isCanProceed()).isFalse();
            assertThat(report.getErrorCount()).isGreaterThanOrEqualTo(1);
            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.ERROR
                    && i.getField().equals("category"));
        }

        @Test
        void validate_validGroup_noIssues() {
            RawGroupRow g = RawGroupRow.builder().rowNum(3).groupId("GRP-001")
                .groupName("Router Bits").categoryName("Cutting Tools").build();

            ValidationReport report = validator.validate(List.of(), List.of(g), List.of());

            assertThat(report.getIssues()).isEmpty();
            assertThat(report.isCanProceed()).isTrue();
        }
    }

    // ========================= Products validation =========================

    @Nested
    @DisplayName("validateProducts")
    class ValidateProducts {

        private RawGroupRow validGroup() {
            return RawGroupRow.builder().rowNum(3).groupId("GRP-001")
                .groupName("Bits").categoryName("Tools").build();
        }

        @Test
        void validate_missingToolNo_errorIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo(null)
                .groupId("GRP-001").build();

            ValidationReport report = validator.validate(List.of(p), List.of(validGroup()), List.of());

            assertThat(report.isCanProceed()).isFalse();
            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.ERROR
                    && i.getField().equals("toolNo"));
        }

        @Test
        void validate_duplicateToolNo_warningIssue() {
            RawProductRow p1 = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").build();
            RawProductRow p2 = RawProductRow.builder().rowNum(4).toolNo("DR001")
                .groupId("GRP-001").description("desc2").build();

            ValidationReport report = validator.validate(
                List.of(p1, p2), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("toolNo")
                    && i.getMessage().contains("Дублирующийся"));
        }

        @Test
        void validate_missingGroupId_errorIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId(null).description("desc").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.isCanProceed()).isFalse();
            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.ERROR
                    && i.getField().equals("groupId"));
        }

        @Test
        void validate_unknownGroupId_errorIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("UNKNOWN").description("desc").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.isCanProceed()).isFalse();
            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.ERROR
                    && i.getField().equals("groupId")
                    && i.getMessage().contains("не найден"));
        }

        @Test
        void validate_missingDescription_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description(null).build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("description"));
        }

        @Test
        void validate_invalidDecimalField_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").dMm("abc").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("D (mm)")
                    && i.getMessage().contains("Нечисловое"));
        }

        @Test
        void validate_rangeDecimalField_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").dMm("3-7").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("D (mm)")
                    && i.getMessage().contains("Диапазонное"));
        }

        @Test
        void validate_validDecimalField_noIssues() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").dMm("12.5").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues().stream()
                .filter(i -> i.getField().equals("D (mm)"))
                .toList()).isEmpty();
        }

        @Test
        void validate_invalidIntegerField_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").flutes("abc").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("Flutes"));
        }

        @Test
        void validate_validIntegerField_noIssues() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").flutes("4").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues().stream()
                .filter(i -> i.getField().equals("Flutes"))
                .toList()).isEmpty();
        }

        @Test
        void validate_unknownCuttingType_warningIssue() {
            when(cuttingTypeNormalizer.normalize("weird type"))
                .thenReturn("weird type");

            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").cuttingType("weird type").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getSeverity() == ValidationIssue.Severity.WARNING
                    && i.getField().equals("cuttingType")
                    && i.getMessage().contains("не найден в словаре"));
        }

        @Test
        void validate_knownCuttingType_noIssue() {
            when(cuttingTypeNormalizer.normalize("Up-Shear"))
                .thenReturn("spiral_upcut");

            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").cuttingType("Up-Shear").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues().stream()
                .filter(i -> i.getField().equals("cuttingType"))
                .toList()).isEmpty();
        }

        @Test
        void validate_validProduct_noIssues() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("Diamond router bit")
                .dMm("12.5").lMm("50").flutes("2").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.isCanProceed()).isTrue();
            assertThat(report.getErrorCount()).isZero();
        }

        @Test
        void validate_unknownHeaders_passedThrough() {
            ValidationReport report = validator.validate(
                List.of(), List.of(), List.of("Unknown Col 1", "Unknown Col 2"));

            assertThat(report.getUnknownHeaders()).containsExactly("Unknown Col 1", "Unknown Col 2");
        }

        @Test
        void validate_nullDecimalField_noIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").dMm(null).build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues().stream()
                .filter(i -> i.getField().equals("D (mm)"))
                .toList()).isEmpty();
        }

        @Test
        void validate_invalidCatalogPage_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").catalogPage("abc").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getField().equals("Catalog Page")
                    && i.getSeverity() == ValidationIssue.Severity.WARNING);
        }

        @Test
        void validate_invalidBladeNo_warningIssue() {
            RawProductRow p = RawProductRow.builder().rowNum(3).toolNo("DR001")
                .groupId("GRP-001").description("desc").bladeNo("CM501217 x2").build();

            ValidationReport report = validator.validate(
                List.of(p), List.of(validGroup()), List.of());

            assertThat(report.getIssues()).anyMatch(i ->
                i.getField().equals("Blade No")
                    && i.getSeverity() == ValidationIssue.Severity.WARNING);
        }
    }
}
