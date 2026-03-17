package com.wpw.pim.service.excel.validation;

import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.dto.RawGroupRow;
import com.wpw.pim.service.excel.dto.RawProductRow;
import com.wpw.pim.service.excel.dto.ValidationIssue;
import com.wpw.pim.service.excel.dto.ValidationIssue.Sheet;
import com.wpw.pim.service.excel.dto.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Предимпортная валидация.
 *
 * Severity.ERROR  — строка не может быть импортирована.
 * Severity.WARNING — строка будет импортирована, но администратор должен это проверить.
 */
@Component
@RequiredArgsConstructor
public class ImportValidator {

    private final CuttingTypeNormalizer cuttingTypeNormalizer;

    public ValidationReport validate(
        List<RawProductRow> products,
        List<RawGroupRow> groups,
        List<String> unknownHeaders
    ) {
        List<ValidationIssue> issues = new ArrayList<>();

        Map<String, RawGroupRow> groupById = groups.stream()
            .filter(g -> g.getGroupId() != null)
            .collect(Collectors.toMap(RawGroupRow::getGroupId, Function.identity(), (a, b) -> a));

        validateGroups(groups, issues, groupById);
        validateProducts(products, issues, groupById);

        long errors   = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count();
        long warnings = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING).count();

        return ValidationReport.builder()
            .totalProductRows(products.size())
            .totalGroupRows(groups.size())
            .errorCount((int) errors)
            .warningCount((int) warnings)
            .canProceed(errors == 0)
            .issues(issues)
            .unknownHeaders(unknownHeaders)
            .build();
    }

    // -------------------------------------------------------------------------
    // Groups
    // -------------------------------------------------------------------------

    private void validateGroups(
        List<RawGroupRow> groups,
        List<ValidationIssue> issues,
        Map<String, RawGroupRow> groupById
    ) {
        Set<String> seenIds = new java.util.HashSet<>();

        for (RawGroupRow g : groups) {
            int row = g.getRowNum();

            // Дублирование groupId в файле
            if (g.getGroupId() != null && !seenIds.add(g.getGroupId())) {
                issues.add(ValidationIssue.warning(Sheet.GROUPS, row, "groupId", g.getGroupId(),
                    "Дублирующийся Group ID — будет использована первая запись"));
            }

            if (blank(g.getGroupName())) {
                issues.add(ValidationIssue.warning(Sheet.GROUPS, row, "groupName", null,
                    "Group Name пустой"));
            }
            if (blank(g.getCategoryName())) {
                issues.add(ValidationIssue.error(Sheet.GROUPS, row, "category", null,
                    "Category не задана — группа не может быть импортирована"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    private void validateProducts(
        List<RawProductRow> products,
        List<ValidationIssue> issues,
        Map<String, RawGroupRow> groupById
    ) {
        Set<String> seenToolNos = new java.util.HashSet<>();

        for (RawProductRow p : products) {
            int row = p.getRowNum();

            // Tool No обязателен
            if (blank(p.getToolNo())) {
                issues.add(ValidationIssue.error(Sheet.PRODUCTS, row, "toolNo", null,
                    "Tool No отсутствует — строка будет пропущена"));
                continue; // дальше строку не валидируем
            }

            // Дублирование в файле
            if (!seenToolNos.add(p.getToolNo())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, "toolNo", p.getToolNo(),
                    "Дублирующийся Tool No в файле — будет использована последняя запись"));
            }

            // Group ID обязателен и должен существовать в листе Groups
            if (blank(p.getGroupId())) {
                issues.add(ValidationIssue.error(Sheet.PRODUCTS, row, "groupId", null,
                    "Group ID отсутствует"));
            } else if (!groupById.containsKey(p.getGroupId())) {
                issues.add(ValidationIssue.error(Sheet.PRODUCTS, row, "groupId", p.getGroupId(),
                    "Group ID «" + p.getGroupId() + "» не найден в листе Product Groups"));
            }

            // Description
            if (blank(p.getDescription())) {
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, "description", null,
                    "Описание отсутствует"));
            }

            // Числовые поля
            validateDecimal(issues, row, "D (mm)", p.getDMm());
            validateDecimal(issues, row, "D1 (mm)", p.getD1Mm());
            validateDecimal(issues, row, "B (mm)", p.getBMm());
            validateDecimal(issues, row, "B1 (mm)", p.getB1Mm());
            validateDecimal(issues, row, "L (mm)", p.getLMm());
            validateDecimal(issues, row, "L1 (mm)", p.getL1Mm());
            validateDecimal(issues, row, "R (mm)", p.getRMm());
            validateDecimal(issues, row, "A (mm)", p.getAMm());
            validateDecimal(issues, row, "Angle (°)", p.getAngleDeg());
            validateDecimal(issues, row, "Shank (mm)", p.getShankMm());
            validateInteger(issues, row, "Flutes", p.getFlutes());
            validateInteger(issues, row, "Blade No", p.getBladeNo());
            validateInteger(issues, row, "Catalog Page", p.getCatalogPage());

            // Cutting Type — предупреждение если не нормализуется в известный код
            if (!blank(p.getCuttingType())) {
                String normalized = cuttingTypeNormalizer.normalize(p.getCuttingType());
                // Если normalize вернул то же самое что пришло (значит не нашёл в словаре)
                if (normalized != null && normalized.equals(p.getCuttingType().toLowerCase().trim())) {
                    // Проверяем что это не известный нормализованный код
                    if (!isKnownNormalizedCode(normalized)) {
                        issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, "cuttingType",
                            p.getCuttingType(),
                            "Cutting Type «" + p.getCuttingType() + "» не найден в словаре нормализации — "
                            + "будет сохранён как есть"));
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    private static final java.util.regex.Pattern RANGE_PATTERN =
        java.util.regex.Pattern.compile("^\\d+(\\.\\d+)?-\\d+(\\.\\d+)?$");

    private static void validateDecimal(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null) return;
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            if (RANGE_PATTERN.matcher(value).matches()) {
                // Диапазоны (напр. "3-7") — допустимы в Excel для наборов, но не могут быть сохранены
                // как одно число. Поле будет сохранено как null.
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                    "Диапазонное значение «" + value + "» — будет сохранено как null"));
            } else {
                // Не числовое значение (напр. "20°48'" или текст) — WARNING, сохраняем null
                issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                    "Нечисловое значение «" + value + "» — будет сохранено как null"));
            }
        }
    }

    private static void validateInteger(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null) return;
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Диапазоны или нечисловые значения (напр. "CM501217 x2") — WARNING
            issues.add(ValidationIssue.warning(Sheet.PRODUCTS, row, field, value,
                "Нечисловое значение «" + value + "» — будет сохранено как null"));
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Нормализованные коды, которые сами по себе являются правильными значениями. */
    private static final Set<String> KNOWN_CODES = Set.of(
        "straight", "spiral_upcut", "spiral_downcut", "compression", "bevel",
        "flush_trim", "conical", "left_hand", "right_hand", "cove", "rabbet",
        "dovetail", "v_groove", "ogee", "slow_spiral", "plunge", "stepped", "wavy"
    );

    private static boolean isKnownNormalizedCode(String code) {
        return KNOWN_CODES.contains(code);
    }
}
