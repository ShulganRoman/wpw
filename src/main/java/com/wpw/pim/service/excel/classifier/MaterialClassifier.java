package com.wpw.pim.service.excel.classifier;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Классифицирует сырое значение колонки Materials на материалы инструмента и заготовки.
 *
 * Правило: если material входит в TOOL_MATERIALS → toolMaterials, иначе → workpieceMaterials.
 * Новые материалы легко добавляются в один из двух словарей.
 */
@Component
public class MaterialClassifier {

    /** Материалы инструмента (из чего сделан сам инструмент). */
    private static final List<String> TOOL_MATERIAL_KEYWORDS = List.of(
        "carbide", "tct", "solid carbide", "hss", "pcd", "diamond", "densimet",
        "steel", "tungsten"
    );

    public record Classification(Set<String> toolMaterials, Set<String> workpieceMaterials) {}

    /**
     * Разбивает строку по запятой и классифицирует каждый элемент.
     */
    public Classification classify(String raw) {
        Set<String> tool = new LinkedHashSet<>();
        Set<String> workpiece = new LinkedHashSet<>();

        if (raw == null || raw.isBlank()) return new Classification(tool, workpiece);

        Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(part -> {
                String lower = part.toLowerCase();
                if (isToolMaterial(lower)) {
                    tool.add(normalizeCode(lower));
                } else {
                    workpiece.add(normalizeCode(lower));
                }
            });

        return new Classification(tool, workpiece);
    }

    private boolean isToolMaterial(String lower) {
        return TOOL_MATERIAL_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /** Нормализует строку материала в код: нижний регистр, пробелы → underscore. */
    private String normalizeCode(String lower) {
        return lower
            .replaceAll("[^a-z0-9 ]", "")  // убираем спецсимволы
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_");
    }
}
