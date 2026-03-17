package com.wpw.pim.service.excel.classifier;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Классифицирует сырое значение колонки Machines на типы станков и бренды.
 *
 * Правило: если значение совпадает с известным брендом → machineBrands,
 * иначе → machineTypes.
 * Новые бренды просто добавляются в BRAND_KEYWORDS.
 */
@Component
public class MachineClassifier {

    /** Известные бренды станков (регистронезависимо). */
    private static final List<String> BRAND_KEYWORDS = List.of(
        "biesse", "bilek", "holzma", "morbidelli", "nottmeyer", "s.c.m.i", "scmi",
        "scheer", "torwegge", "ayen", "homag", "ima", "weeke"
    );

    public record Classification(Set<String> machineTypes, Set<String> machineBrands) {}

    public Classification classify(String raw) {
        Set<String> types = new LinkedHashSet<>();
        Set<String> brands = new LinkedHashSet<>();

        if (raw == null || raw.isBlank()) return new Classification(types, brands);

        Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(part -> {
                String lower = part.toLowerCase();
                if (isBrand(lower)) {
                    brands.add(normalizeCode(lower));
                } else {
                    types.add(normalizeCode(lower));
                }
            });

        return new Classification(types, brands);
    }

    private boolean isBrand(String lower) {
        return BRAND_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String normalizeCode(String lower) {
        return lower
            .replaceAll("[^a-z0-9 ]", "")
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_");
    }
}
