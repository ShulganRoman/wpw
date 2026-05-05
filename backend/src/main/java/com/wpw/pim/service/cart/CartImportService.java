package com.wpw.pim.service.cart;

import com.wpw.pim.domain.cart.CartItem;
import com.wpw.pim.domain.cart.CartItemId;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.cart.CartItemRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.cart.CartImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartImportService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final DealerRepository dealerRepository;
    private final CartService cartService;

    /**
     * Parses Excel, resolves toolNos to products, and upserts cart items.
     * Quantity from file replaces existing quantity.
     * Returns a result with counters and a list of unrecognised SKUs.
     */
    @Transactional
    public CartImportResult importFromExcel(UUID dealerId, MultipartFile file) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dealer not found"));

        List<ExcelRow> rows = parseExcel(file);

        // Batch-load products by toolNo (case-insensitive)
        Set<String> requestedToolNos = rows.stream()
                .map(r -> r.toolNo().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, Product> productByToolNo = productRepository
                .findByToolNoUpperIn(requestedToolNos)
                .stream()
                .collect(Collectors.toMap(p -> p.getToolNo().toUpperCase(), p -> p));

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int replaced = 0;

        for (ExcelRow row : rows) {
            if (row.rowNum() <= 2) continue;

            String key = row.toolNo().toUpperCase();
            Product product = productByToolNo.get(key);

            if (product == null) {
                errors.add("Row " + row.rowNum() + ": Tool No \"" + row.toolNo() + "\" not found");
                continue;
            }

            if (row.qty() <= 0) {
                errors.add("Row " + row.rowNum() + ": invalid quantity " + row.qty() + " for \"" + row.toolNo() + "\"");
                continue;
            }

            CartItemId itemId = new CartItemId(dealerId, product.getId());
            CartItem existing = cartItemRepository.findById(itemId).orElse(null);

            if (existing != null) {
                existing.setQty(row.qty());
                cartItemRepository.save(existing);
                replaced++;
            } else {
                cartItemRepository.save(new CartItem(dealer, product, row.qty()));
                imported++;
            }
        }

        return new CartImportResult(imported, replaced, errors, cartService.getCart(dealerId));
    }

    // ── Excel parsing ─────────────────────────────────────────────────────────

    private List<ExcelRow> parseExcel(MultipartFile file) {
        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            List<ExcelRow> rows = new ArrayList<>();

            // Find header row (first row where column A contains "Tool No")
            int dataStartRow = findDataStartRow(sheet);

            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String toolNo = cellString(row.getCell(0));
                if (toolNo.isBlank()) continue;

                int qty = (int) cellNumeric(row.getCell(1), 1);

                rows.add(new ExcelRow(i + 1, toolNo.trim(), qty));
            }

            return rows;

        } catch (Exception e) {
            log.error("Failed to parse cart import Excel: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to parse Excel file: " + e.getMessage());
        }
    }

    /**
     * Finds the first row after the header row (skips instruction + header).
     * Looks for a row where cell A contains "tool no" (case-insensitive).
     * Falls back to row index 2 (row 3 in Excel) matching the template layout.
     */
    private int findDataStartRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 5); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String val = cellString(row.getCell(0)).toLowerCase();
            if (val.contains("tool no") || val.contains("tool_no")) {
                return i + 1;
            }
        }
        return 2; // default: data starts at row 3 (0-indexed = 2)
    }

    private String cellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private double cellNumeric(Cell cell, double defaultVal) {
        if (cell == null) return defaultVal;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield defaultVal;
                }
            }
            default -> defaultVal;
        };
    }

    private record ExcelRow(int rowNum, String toolNo, int qty) {
    }
}
