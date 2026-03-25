package com.wpw.pim.service.excel.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Строка листа Product Groups в сыром виде.
 */
@Getter
@Builder
public class RawGroupRow {
    private final int    rowNum;
    private final String groupId;
    private final String groupCode;
    private final String groupName;
    private final String categoryName;
    private final String shortDescription;
    private final String applications;
    private final String materials;
    private final String machines;
    private final String catalogPages;
}
