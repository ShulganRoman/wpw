package com.wpw.pim.service.excel.validation;

import com.wpw.pim.service.excel.dto.RawV4Row;
import com.wpw.pim.service.excel.dto.ValidationIssue;
import com.wpw.pim.service.excel.dto.ValidationIssue.Sheet;
import com.wpw.pim.service.excel.dto.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Предимпортная валидация для формата v4.
 * <p>
 * В отличие от {@link ImportValidator}, не требует листа Groups —
 * группы определяются по паре (Category, Group Name) из строк продуктов.
 * </p>
 *
 * <ul>
 *   <li>ERROR — строка не может быть импортирована</li>
 *   <li>WARNING — строка будет импортирована, но требует внимания</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ImportV4Validator {

    private static final Set<String> VALID_STATUSES = Set.of("active", "draft", "discontinued");
    private static final Set<String> VALID_PRODUCT_TYPES = Set.of("main", "spare", "accessory");
    private static final Set<String> VALID_STOCK_STATUSES = Set.of("in_stock", "low_stock", "out_of_stock");
    private static final Set<String> VALID_ROTATION_DIRS = Set.of("right", "left", "both");
    private static final Set<String> VALID_BORE_TYPES = Set.of("shank", "bore");

    /**
     * Backwards-compatible overload: validates без dry-run контекста.
     * dry-run-счётчики всё равно посчитаются (всё пойдёт в "будет создано", БД не запрашивается).
     */
    public ValidationReport validate(List<RawV4Row> rows, List<String> unknownHeaders) {
        return validate(rows, unknownHeaders, Collections.emptySet());
    }

    /**
     * Валидирует список строк и неизвестные заголовки, дополнительно считая dry-run превью.
     *
     * @param rows                    распарсенные строки из листа Products
     * @param unknownHeaders          заголовки, не распознанные конфигом
     * @param existingToolNosUpper    upper-case toolNo, которые уже есть в БД
     *                                (нужно, чтобы отличать «будет обновлено» от «будет создано»)
     * @return отчёт с ошибками, предупреждениями и dry-run превью (включая готовый markdown)
     */
    public ValidationReport validate(List<RawV4Row> rows,
                                     List<String> unknownHeaders,
                                     Set<String> existingToolNosUpper) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> seenToolNos = new HashSet<>();
        List<String> toCreate = new ArrayList<>();
        List<String> toUpdate = new ArrayList<>();
        List<String> toSkip   = new ArrayList<>();

        for (RawV4Row row : rows) {
            int rowNum = row.getRowNum();

            // Tool No обязателен
            if (blank(row.getToolNo())) {
                issues.add(ValidationIssue.error(Sheet.PRODUCTS, rowNum, "toolNo", null,
                    "Tool No is missing"));
                toSkip.add("Row " + rowNum + ": Tool No is missing");
                continue;
            }

            // Дублирование toolNo в файле — case-insensitive: execute использует upsert по UPPER(toolNo).
            // Первая встреча в dry-run считается как create/update, последующие не дублируются.
            String toolNoKey = row.getToolNo().trim().toUpperCase();
            boolean isDuplicateInFile = !seenToolNos.add(toolNoKey);
            if (isDuplicateInFile) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "toolNo", row.getToolNo(),
                    "Duplicate Tool No in file"));
            }

            // Category
            if (blank(row.getCategoryName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "category", null,
                    "Category not set"));
            }

            // Group Name
            if (blank(row.getGroupName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "groupName", null,
                    "Group Name not set"));
            }

            // Name
            if (blank(row.getName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "name", null,
                    "Name is missing"));
            }

            // Decimal fields
            validateDecimal(issues, rowNum, "D (mm)", row.getDMm());
            validateDecimal(issues, rowNum, "D1 (mm)", row.getD1Mm());
            validateDecimal(issues, rowNum, "D2 (mm)", row.getD2Mm());
            validateDecimal(issues, rowNum, "B / Cut. Length (mm)", row.getBMm());
            validateDecimal(issues, rowNum, "B1 (mm)", row.getB1Mm());
            validateDecimal(issues, rowNum, "L / Total (mm)", row.getLMm());
            validateDecimal(issues, rowNum, "L1 (mm)", row.getL1Mm());
            validateDecimal(issues, rowNum, "R (mm)", row.getRMm());
            validateDecimal(issues, rowNum, "A (mm)", row.getAMm());
            validateDecimal(issues, rowNum, "Angle (\u00b0)", row.getAngleDeg());
            validateDecimal(issues, rowNum, "Shank (mm)", row.getShankMm());

            // Integer fields
            validateInteger(issues, rowNum, "Flutes", row.getFlutes());
            validateInteger(issues, rowNum, "Blade No", row.getBladeNo());
            validateInteger(issues, rowNum, "Catalog Page", row.getCatalogPage());
            validateInteger(issues, rowNum, "Weight (g)", row.getWeightG());
            validateInteger(issues, rowNum, "Package Qty", row.getPkgQty());
            validateInteger(issues, rowNum, "Carton Qty", row.getCartonQty());
            validateInteger(issues, rowNum, "Stock Qty", row.getStockQty());

            // Enum fields
            validateEnum(issues, rowNum, "status", row.getStatus(), VALID_STATUSES);
            validateEnum(issues, rowNum, "productType", row.getProductType(), VALID_PRODUCT_TYPES);
            validateEnum(issues, rowNum, "stockStatus", row.getStockStatus(), VALID_STOCK_STATUSES);
            validateEnum(issues, rowNum, "rotationDirection", row.getRotationDirection(), VALID_ROTATION_DIRS);
            validateEnum(issues, rowNum, "boreType", row.getBoreType(), VALID_BORE_TYPES);

            // Dry-run accounting — только для первой встречи toolNo в файле,
            // execute использует upsert по toolNo, дубль не создаёт второй продукт.
            if (!isDuplicateInFile) {
                if (existingToolNosUpper.contains(toolNoKey)) {
                    toUpdate.add(row.getToolNo());
                } else {
                    toCreate.add(row.getToolNo());
                }
            }
        }

        long errors = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count();
        long warnings = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING).count();

        String dryRunReport = buildDryRunReport(rows.size(), toCreate, toUpdate, toSkip, errors, warnings, unknownHeaders);

        return ValidationReport.builder()
            .totalProductRows(rows.size())
            .totalGroupRows(0)
            .errorCount((int) errors)
            .warningCount((int) warnings)
            .canProceed(errors == 0)
            .issues(issues)
            .unknownHeaders(unknownHeaders)
            .wouldCreate(toCreate.size())
            .wouldUpdate(toUpdate.size())
            .wouldSkip(toSkip.size())
            .dryRunReport(dryRunReport)
            .build();
    }

    // -------------------------------------------------------------------------
    // Markdown dry-run report
    // -------------------------------------------------------------------------

    private static String buildDryRunReport(int total,
                                            List<String> toCreate,
                                            List<String> toUpdate,
                                            List<String> toSkip,
                                            long errors,
                                            long warnings,
                                            List<String> unknownHeaders) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String status = errors == 0
            ? "Passed"
            : "Failed (" + errors + " errors)";

        StringBuilder sb = new StringBuilder();
        sb.append("# WPW PIM — Import Validation Report (Dry Run)\n\n");
        sb.append("**Generated:** ").append(now).append("  \n");
        sb.append("**Total product rows:** ").append(total).append("  \n");
        sb.append("**Validation:** ").append(status).append("  \n");
        sb.append("**Warnings:** ").append(warnings).append("\n\n");

        sb.append("## Summary\n\n");
        sb.append("| Action | Count |\n");
        sb.append("|---|---:|\n");
        sb.append("| Will be created | ").append(toCreate.size()).append(" |\n");
        sb.append("| Will be updated | ").append(toUpdate.size()).append(" |\n");
        sb.append("| Will be skipped | ").append(toSkip.size()).append(" |\n\n");

        appendList(sb, "Will be created", toCreate);
        appendList(sb, "Will be updated", toUpdate);
        appendList(sb, "Will be skipped", toSkip);

        if (unknownHeaders != null && !unknownHeaders.isEmpty()) {
            sb.append("## Unknown headers (").append(unknownHeaders.size()).append(")\n\n");
            sb.append("> Columns from the file that the importer did not recognise — check for typos or renamed columns.\n\n");
            for (String h : unknownHeaders) {
                sb.append("- `").append(h).append("`\n");
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String title, List<String> items) {
        if (items.isEmpty()) return;
        sb.append("## ").append(title).append(" (").append(items.size()).append(")\n\n");
        for (String it : items) {
            sb.append("- ").append(it).append('\n');
        }
        sb.append('\n');
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы валидации
    // -------------------------------------------------------------------------

    private static void validateDecimal(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null || value.isBlank()) return;
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Non-numeric value \u00ab" + value + "\u00bb — will be saved as null"));
        }
    }

    private static void validateInteger(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null || value.isBlank()) return;
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Non-numeric value \u00ab" + value + "\u00bb — will be saved as null"));
        }
    }

    private static void validateEnum(List<ValidationIssue> issues, int row,
                                     String field, String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return;
        if (!allowed.contains(value.toLowerCase().trim())) {
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Unknown value \u00ab" + value + "\u00bb — allowed: " + allowed));
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
