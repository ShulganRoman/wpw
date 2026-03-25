package com.wpw.pim.service.excel.parser;

import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.RawGroupRow;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсер листа Product Groups.
 */
@Component
@RequiredArgsConstructor
public class GroupsSheetParser {

    private final ExcelImportProperties props;

    public List<RawGroupRow> parse(Sheet sheet, FormulaEvaluator evaluator) {
        ExcelImportProperties.GroupsSheet cfg = props.getGroupsSheet();
        int headerRowIdx = props.getHeaderRow() - 1;
        int dataStartIdx = props.getDataStartRow() - 1;

        Row headerRow = sheet.getRow(headerRowIdx);
        ColumnIndex idx = new ColumnIndex(headerRow);

        List<RawGroupRow> result = new ArrayList<>();
        for (int i = dataStartIdx; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String groupId = CellReader.read(row, idx.get(cfg.getGroupId()), evaluator);
            if (groupId == null) continue;

            result.add(RawGroupRow.builder()
                .rowNum(i + 1)
                .groupId(groupId)
                .groupCode(CellReader.read(row, idx.get(cfg.getGroupCode()), evaluator))
                .groupName(CellReader.read(row, idx.get(cfg.getGroupName()), evaluator))
                .categoryName(CellReader.read(row, idx.get(cfg.getCategory()), evaluator))
                .shortDescription(CellReader.read(row, idx.get(cfg.getShortDescription()), evaluator))
                .applications(CellReader.read(row, idx.get(cfg.getApplications()), evaluator))
                .materials(CellReader.read(row, idx.get(cfg.getMaterials()), evaluator))
                .machines(CellReader.read(row, idx.get(cfg.getMachines()), evaluator))
                .catalogPages(CellReader.read(row, idx.get(cfg.getCatalogPages()), evaluator))
                .build());
        }
        return result;
    }
}
