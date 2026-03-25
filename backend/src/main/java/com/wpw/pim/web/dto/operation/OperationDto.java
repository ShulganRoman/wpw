package com.wpw.pim.web.dto.operation;

import java.util.Map;

public record OperationDto(
    String code,
    String name,
    String nameKey,
    int sortOrder
) {
    private static final Map<String, String> NAME_MAP = Map.of(
        "nesting", "Nesting",
        "profiling", "Profiling",
        "grooving", "Grooving",
        "trimming", "Trimming",
        "jointing", "Jointing",
        "drilling", "Drilling",
        "surface", "Surface Processing"
    );

    public static OperationDto from(com.wpw.pim.domain.operation.Operation op) {
        String name = NAME_MAP.getOrDefault(op.getCode(),
            op.getCode().substring(0, 1).toUpperCase() + op.getCode().substring(1));
        return new OperationDto(op.getCode(), name, op.getNameKey(), op.getSortOrder());
    }
}
