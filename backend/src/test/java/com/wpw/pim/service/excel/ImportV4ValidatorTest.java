package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.dto.RawV4Row;
import com.wpw.pim.service.excel.dto.ValidationIssue;
import com.wpw.pim.service.excel.dto.ValidationReport;
import com.wpw.pim.service.excel.validation.ImportV4Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportV4ValidatorTest {

    private final ImportV4Validator validator = new ImportV4Validator();

    private RawV4Row.RawV4RowBuilder validRow(int rowNum) {
        return RawV4Row.builder()
            .rowNum(rowNum)
            .toolNo("WPW-001")
            .name("Test Product")
            .categoryName("Router Bits")
            .groupName("Straight Bits")
            .status("active")
            .productType("main")
            .dMm("12.5")
            .flutes("2");
    }

    @Test
    @DisplayName("valid row produces no errors")
    void validate_validRow_noErrors() {
        List<RawV4Row> rows = List.of(validRow(3).build());
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getErrorCount()).isZero();
        assertThat(report.isCanProceed()).isTrue();
    }

    @Test
    @DisplayName("empty toolNo produces ERROR")
    void validate_emptyToolNo_error() {
        List<RawV4Row> rows = List.of(
            RawV4Row.builder().rowNum(3).build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getErrorCount()).isEqualTo(1);
        assertThat(report.isCanProceed()).isFalse();
        assertThat(report.getIssues().get(0).getSeverity()).isEqualTo(ValidationIssue.Severity.ERROR);
        assertThat(report.getIssues().get(0).getField()).isEqualTo("toolNo");
    }

    @Test
    @DisplayName("duplicate toolNo produces WARNING")
    void validate_duplicateToolNo_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).build(),
            validRow(4).build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getWarningCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("toolNo") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("empty categoryName produces WARNING")
    void validate_emptyCategory_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).categoryName(null).build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("category") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("empty groupName produces WARNING")
    void validate_emptyGroupName_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).groupName(null).build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("groupName") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("invalid status produces WARNING")
    void validate_invalidStatus_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).status("unknown_status").build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("status") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("invalid productType produces WARNING")
    void validate_invalidProductType_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).productType("invalid_type").build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("productType") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("invalid stockStatus produces WARNING")
    void validate_invalidStockStatus_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).stockStatus("plenty").build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("stockStatus") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("non-numeric dMm produces WARNING")
    void validate_nonNumericDMm_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).dMm("abc").build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("D (mm)") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("non-numeric flutes produces WARNING")
    void validate_nonNumericFlutes_warning() {
        List<RawV4Row> rows = List.of(
            validRow(3).flutes("two").build()
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.getIssues()).anyMatch(i ->
            i.getField().equals("Flutes") && i.getSeverity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    @DisplayName("canProceed is false when ERROR exists")
    void validate_errorPresent_cannotProceed() {
        List<RawV4Row> rows = List.of(
            RawV4Row.builder().rowNum(3).build() // no toolNo → ERROR
        );
        ValidationReport report = validator.validate(rows, Collections.emptyList());

        assertThat(report.isCanProceed()).isFalse();
    }
}
