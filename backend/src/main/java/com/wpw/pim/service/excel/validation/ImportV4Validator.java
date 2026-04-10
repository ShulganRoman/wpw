package com.wpw.pim.service.excel.validation;

import com.wpw.pim.service.excel.dto.RawV4Row;
import com.wpw.pim.service.excel.dto.ValidationIssue;
import com.wpw.pim.service.excel.dto.ValidationIssue.Sheet;
import com.wpw.pim.service.excel.dto.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
     * Валидирует список строк и неизвестные заголовки.
     *
     * @param rows           распарсенные строки из листа Products
     * @param unknownHeaders заголовки, не распознанные конфигом
     * @return отчёт с ошибками и предупреждениями
     */
    public ValidationReport validate(List<RawV4Row> rows, List<String> unknownHeaders) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> seenToolNos = new HashSet<>();

        for (RawV4Row row : rows) {
            int rowNum = row.getRowNum();

            // Tool No обязателен
            if (blank(row.getToolNo())) {
                issues.add(ValidationIssue.error(Sheet.PRODUCTS, rowNum, "toolNo", null,
                    "Tool No отсутствует"));
                continue;
            }

            // Дублирование toolNo в файле
            if (!seenToolNos.add(row.getToolNo())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "toolNo", row.getToolNo(),
                    "Дублирующийся Tool No в файле"));
            }

            // Category
            if (blank(row.getCategoryName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "category", null,
                    "Category не задана"));
            }

            // Group Name
            if (blank(row.getGroupName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "groupName", null,
                    "Group Name не задан"));
            }

            // Name
            if (blank(row.getName())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, rowNum, "name", null,
                    "Name отсутствует"));
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
        }

        long errors = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count();
        long warnings = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING).count();

        return ValidationReport.builder()
            .totalProductRows(rows.size())
            .totalGroupRows(0)
            .errorCount((int) errors)
            .warningCount((int) warnings)
            .canProceed(errors == 0)
            .issues(issues)
            .unknownHeaders(unknownHeaders)
            .build();
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
                "Нечисловое значение \u00ab" + value + "\u00bb \u2014 будет сохранено как null"));
        }
    }

    private static void validateInteger(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null || value.isBlank()) return;
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Нечисловое значение \u00ab" + value + "\u00bb \u2014 будет сохранено как null"));
        }
    }

    private static void validateEnum(List<ValidationIssue> issues, int row,
                                     String field, String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return;
        if (!allowed.contains(value.toLowerCase().trim())) {
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Неизвестное значение \u00ab" + value + "\u00bb \u2014 допустимые: " + allowed));
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
