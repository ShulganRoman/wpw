package com.wpw.pim.web.dto.catalog;
import java.util.List;
import java.util.UUID;
public record ReorderRequest(List<ReorderItem> items) {
    public record ReorderItem(UUID id, int sortOrder) {}
}
