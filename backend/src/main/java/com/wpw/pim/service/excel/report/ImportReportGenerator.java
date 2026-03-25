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
        line(md, "**Дата импорта:** " + s.getImportedAt());
        line(md, "**Длительность:** " + formatDuration(s.getDuration()));
        md.append("\n");

        header2(md, "Summary");
        table(md,
            List.of("Метрика", "Кол-во"),
            List.of(
                row("Всего строк товаров в файле", s.getTotalProductRows()),
                row("Товаров создано",              s.getProductsCreated()),
                row("Товаров обновлено",            s.getProductsUpdated()),
                row("Строк пропущено",              s.getProductsSkipped()),
                row("Ошибок при выполнении",        s.getExecutionErrors().size())
            )
        );
        md.append("\n");

        header2(md, "Структура каталога");
        table(md,
            List.of("Объект", "Создано", "Найдено"),
            List.of(
                row3("Разделы (Sections)",     s.getSectionsCreated(),    "—"),
                row3("Категории (Categories)", s.getCategoriesCreated(),  s.getCategoriesFound()),
                row3("Группы (Product Groups)", s.getGroupsCreated(),     s.getGroupsFound())
            )
        );
        md.append("\n");

        if (!s.getExecutionErrors().isEmpty()) {
            header2(md, "Ошибки выполнения");
            md.append("> ⚠️ Следующие строки не были импортированы:\n\n");
            s.getExecutionErrors().forEach(e -> md.append("- ").append(e).append("\n"));
            md.append("\n");
        }

        if (!s.getExecutionWarnings().isEmpty()) {
            header2(md, "Предупреждения");
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
