package com.wpw.pim.web.dto.operation;

public record OperationDto(
    String code,
    String name,
    int sortOrder
) {
    public static OperationDto from(com.wpw.pim.domain.operation.Operation op) {
        return new OperationDto(op.getCode(), op.getName(), op.getSortOrder());
    }
}
