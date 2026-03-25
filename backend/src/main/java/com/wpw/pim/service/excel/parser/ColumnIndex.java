package com.wpw.pim.service.excel.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Индекс колонок листа — строит Map<заголовок, индекс> из строки заголовков.
 * Благодаря этому порядок колонок в Excel не важен; достаточно правильных заголовков.
 */
public class ColumnIndex {

    private final Map<String, Integer> nameToIndex = new HashMap<>();

    public ColumnIndex(Row headerRow) {
        if (headerRow == null) return;
        for (Cell cell : headerRow) {
            String name = cellString(cell);
            if (name != null) {
                nameToIndex.put(name, cell.getColumnIndex());
            }
        }
    }

    /** Возвращает индекс колонки или -1 если не найдена. */
    public int get(String headerName) {
        return nameToIndex.getOrDefault(headerName, -1);
    }

    public boolean has(String headerName) {
        return nameToIndex.containsKey(headerName);
    }

    /** Набор всех заголовков, найденных в файле. */
    public Set<String> foundHeaders() {
        return nameToIndex.keySet();
    }

    private static String cellString(Cell cell) {
        if (cell == null) return null;
        String v = cell.getStringCellValue().trim();
        return v.isEmpty() ? null : v;
    }
}
