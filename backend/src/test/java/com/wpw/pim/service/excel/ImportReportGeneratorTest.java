package com.wpw.pim.service.excel;

import com.wpw.pim.service.excel.dto.ImportStats;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link ImportReportGenerator}.
 * Проверяют генерацию Markdown-отчёта по результатам импорта.
 */
class ImportReportGeneratorTest {

    private ImportReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ImportReportGenerator();
    }

    @Test
    @DisplayName("generate — includes header and date")
    void generate_containsHeaderAndDate() {
        ImportStats stats = buildStats(Duration.ofMillis(500), 10, 5, 3, 2);
        String md = generator.generate(stats);

        assertThat(md).contains("# WPW PIM — Import Report");
        assertThat(md).contains("**Import date:**");
    }

    @Test
    @DisplayName("generate — includes Summary table with counts")
    void generate_containsSummaryTable() {
        ImportStats stats = buildStats(Duration.ofMillis(500), 100, 80, 15, 5);
        String md = generator.generate(stats);

        assertThat(md).contains("## Summary");
        assertThat(md).contains("Total product rows in file");
        assertThat(md).contains("**100**");
        assertThat(md).contains("Products created");
        assertThat(md).contains("**80**");
        assertThat(md).contains("Products updated");
        assertThat(md).contains("**15**");
        assertThat(md).contains("Rows skipped");
        assertThat(md).contains("**5**");
    }

    @Test
    @DisplayName("generate — includes catalog structure")
    void generate_containsCatalogStructure() {
        ImportStats stats = ImportStats.builder()
            .importedAt("2024-01-01 12:00:00")
            .duration(Duration.ofMillis(100))
            .totalProductRows(0).productsCreated(0).productsUpdated(0).productsSkipped(0)
            .sectionsCreated(1).categoriesCreated(5).categoriesFound(3)
            .groupsCreated(10).groupsFound(7)
            .executionErrors(List.of()).executionWarnings(List.of())
            .build();

        String md = generator.generate(stats);

        assertThat(md).contains("## Catalog Structure");
        assertThat(md).contains("Sections");
        assertThat(md).contains("Categories");
        assertThat(md).contains("Product Groups");
    }

    @Test
    @DisplayName("generate — displays execution errors if present")
    void generate_includesExecutionErrors() {
        ImportStats stats = ImportStats.builder()
            .importedAt("2024-01-01 12:00:00")
            .duration(Duration.ofMillis(100))
            .totalProductRows(5).productsCreated(3).productsUpdated(0).productsSkipped(2)
            .sectionsCreated(0).categoriesCreated(0).categoriesFound(0)
            .groupsCreated(0).groupsFound(0)
            .executionErrors(List.of("Product DR001: group not found", "Product DR002: parsing error"))
            .executionWarnings(List.of())
            .build();

        String md = generator.generate(stats);

        assertThat(md).contains("## Execution Errors");
        assertThat(md).contains("Product DR001: group not found");
        assertThat(md).contains("Product DR002: parsing error");
    }

    @Test
    @DisplayName("generate — displays warnings if present")
    void generate_includesWarnings() {
        ImportStats stats = ImportStats.builder()
            .importedAt("2024-01-01 12:00:00")
            .duration(Duration.ofMillis(100))
            .totalProductRows(5).productsCreated(5).productsUpdated(0).productsSkipped(0)
            .sectionsCreated(0).categoriesCreated(0).categoriesFound(0)
            .groupsCreated(0).groupsFound(0)
            .executionErrors(List.of())
            .executionWarnings(List.of("Field D empty for DR003"))
            .build();

        String md = generator.generate(stats);

        assertThat(md).contains("## Warnings");
        assertThat(md).contains("Field D empty for DR003");
    }

    @Test
    @DisplayName("generate — no errors/warnings — no error sections")
    void generate_noErrorsOrWarnings_noErrorSections() {
        ImportStats stats = buildStats(Duration.ofMillis(100), 10, 10, 0, 0);
        String md = generator.generate(stats);

        assertThat(md).doesNotContain("## Execution Errors");
        assertThat(md).doesNotContain("## Warnings");
    }

    @Test
    @DisplayName("generate — duration < 1s displayed in ms")
    void generate_shortDuration_displayedInMs() {
        ImportStats stats = buildStats(Duration.ofMillis(450), 10, 10, 0, 0);
        String md = generator.generate(stats);

        assertThat(md).contains("450ms");
    }

    @Test
    @DisplayName("generate — duration >= 1s displayed in seconds")
    void generate_longDuration_displayedInSeconds() {
        ImportStats stats = buildStats(Duration.ofMillis(2500), 10, 10, 0, 0);
        String md = generator.generate(stats);

        assertThat(md).containsPattern("2[.,]50s");
    }

    @Test
    @DisplayName("generate — null duration displayed as n/a")
    void generate_nullDuration_displayedAsNA() {
        ImportStats stats = ImportStats.builder()
            .importedAt("2024-01-01 12:00:00")
            .duration(null)
            .totalProductRows(0).productsCreated(0).productsUpdated(0).productsSkipped(0)
            .sectionsCreated(0).categoriesCreated(0).categoriesFound(0)
            .groupsCreated(0).groupsFound(0)
            .executionErrors(List.of()).executionWarnings(List.of())
            .build();

        String md = generator.generate(stats);
        assertThat(md).contains("n/a");
    }

    private ImportStats buildStats(Duration duration, int total, int created, int updated, int skipped) {
        return ImportStats.builder()
            .importedAt("2024-01-01 12:00:00")
            .duration(duration)
            .totalProductRows(total)
            .productsCreated(created)
            .productsUpdated(updated)
            .productsSkipped(skipped)
            .sectionsCreated(0)
            .categoriesCreated(0)
            .categoriesFound(0)
            .groupsCreated(0)
            .groupsFound(0)
            .executionErrors(List.of())
            .executionWarnings(List.of())
            .build();
    }
}
