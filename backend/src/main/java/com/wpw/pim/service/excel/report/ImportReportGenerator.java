package com.wpw.pim.service.excel.report;

import com.wpw.pim.service.excel.dto.ImportStats;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Генерирует Markdown-отчёт об импорте.
 */
@Component
public class ImportReportGenerator {

    public String generate(ImportStats s) {
        StringBuilder md = new StringBuilder();

        header(md, "WPW PIM — Import Report");
        md.append("\n");
        line(md, "**Import date:** " + s.getImportedAt());
        line(md, "**Duration:** " + formatDuration(s.getDuration()));
        md.append("\n");

        header2(md, "Summary");
        table(md,
            List.of("Metric", "Count"),
            List.of(
                row("Total product rows in file", s.getTotalProductRows()),
                row("Products created",           s.getProductsCreated()),
                row("Products updated",           s.getProductsUpdated()),
                row("Rows skipped",               s.getProductsSkipped()),
                row("Execution errors",           s.getExecutionErrors().size())
            )
        );
        md.append("\n");

        header2(md, "Catalog Structure");
        table(md,
            List.of("Object", "Created", "Found"),
            List.of(
                row3("Sections",      s.getSectionsCreated(),    "—"),
                row3("Categories",    s.getCategoriesCreated(),  s.getCategoriesFound()),
                row3("Product Groups", s.getGroupsCreated(),     s.getGroupsFound())
            )
        );
        md.append("\n");

        if (!s.getExecutionErrors().isEmpty()) {
            header2(md, "Execution Errors");
            md.append("> ⚠️ The following rows were not imported:\n\n");
            s.getExecutionErrors().forEach(e -> md.append("- ").append(e).append("\n"));
            md.append("\n");
        }

        if (!s.getExecutionWarnings().isEmpty()) {
            header2(md, "Warnings");
            s.getExecutionWarnings().forEach(w -> md.append("- ").append(w).append("\n"));
            md.append("\n");
        }

        return md.toString();
    }

    // -------------------------------------------------------------------------

    private static void header(StringBuilder sb, String text) {
        sb.append("# ").append(text).append("\n");
    }

    private static void header2(StringBuilder sb, String text) {
        sb.append("## ").append(text).append("\n\n");
    }

    private static void line(StringBuilder sb, String text) {
        sb.append(text).append("\n");
    }

    private static void table(StringBuilder sb, List<String> headers, List<String> rows) {
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
        sb.append("|").append("---|".repeat(headers.size())).append("\n");
        rows.forEach(r -> sb.append(r).append("\n"));
        sb.append("\n");
    }

    private static String row(String label, int value) {
        return "| " + label + " | **" + value + "** |";
    }

    private static String row3(String label, Object v1, Object v2) {
        return "| " + label + " | " + v1 + " | " + v2 + " |";
    }

    private static String formatDuration(java.time.Duration d) {
        if (d == null) return "n/a";
        long ms = d.toMillis();
        if (ms < 1000) return ms + "ms";
        return String.format("%.2fs", ms / 1000.0);
    }
}
