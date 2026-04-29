package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final PriceListRepository priceListRepo;
    private final PriceListItemRepository itemRepo;
    private final ProductRepository productRepo;

    public PriceList getOrCreateStockList() {
        return priceListRepo.findFirstByType("stock").orElseGet(() -> {
            PriceList pl = new PriceList();
            pl.setName("Stock");
            pl.setType("stock");
            pl.setCurrency(new com.wpw.pim.domain.pricing.Currency());
            // currency will be set separately; use USD as default placeholder
            // real seed done in AdminInitializer
            return priceListRepo.save(pl);
        });
    }

    @Transactional(readOnly = true)
    public List<PriceListItemDto> getItems() {
        PriceList stock = priceListRepo.findFirstByType("stock")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price list not initialised"));
        return itemRepo.findByPriceListIdOrderByIdMinQtyAsc(stock.getId()).stream()
            .map(i -> new PriceListItemDto(i.getProduct().getToolNo(), i.getId().getMinQty(), i.getPrice()))
            .toList();
    }

    @Transactional
    public PriceListItemDto upsertItem(PriceListItemRequest req) {
        PriceList stock = priceListRepo.findFirstByType("stock")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price list not initialised"));
        Product product = productRepo.findByToolNo(req.toolNo())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + req.toolNo()));

        PriceListItemId id = new PriceListItemId(stock.getId(), product.getId(), req.minQty());
        PriceListItem item = itemRepo.findById(id).orElse(new PriceListItem());
        item.setId(id);
        item.setPriceList(stock);
        item.setProduct(product);
        item.setPrice(req.price());
        itemRepo.save(item);
        return new PriceListItemDto(req.toolNo(), req.minQty(), req.price());
    }

    @Transactional
    public void deleteItem(String toolNo, int minQty) {
        PriceList stock = priceListRepo.findFirstByType("stock")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price list not initialised"));
        Product product = productRepo.findByToolNo(toolNo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + toolNo));
        itemRepo.deleteById(new PriceListItemId(stock.getId(), product.getId(), minQty));
    }

    @Transactional
    public PriceImportResult importExcel(MultipartFile file) throws IOException {
        PriceList stock = priceListRepo.findFirstByType("stock")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock price list not initialised"));

        List<String> errors = new ArrayList<>();
        List<PriceListItem> toSave = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String toolNo = cellStr(row.getCell(0));
                    int minQty = (int) row.getCell(1).getNumericCellValue();
                    BigDecimal price = BigDecimal.valueOf(row.getCell(2).getNumericCellValue());
                    if (toolNo.isBlank()) continue;

                    final int rowNum = i;
                    final String finalToolNo = toolNo;
                    final int finalMinQty = minQty;
                    final BigDecimal finalPrice = price;
                    productRepo.findByToolNo(toolNo).ifPresentOrElse(product -> {
                        PriceListItem item = new PriceListItem();
                        item.setId(new PriceListItemId(stock.getId(), product.getId(), finalMinQty));
                        item.setPriceList(stock);
                        item.setProduct(product);
                        item.setPrice(finalPrice);
                        toSave.add(item);
                    }, () -> errors.add("Row " + (rowNum + 1) + ": product not found: " + finalToolNo));
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        // replace-all for stock: delete existing, insert new
        itemRepo.deleteByPriceListId(stock.getId());
        itemRepo.saveAll(toSave);

        return new PriceImportResult(toSave.size(), errors.size(), errors);
    }

    public byte[] export() throws IOException {
        List<PriceListItemDto> items = getItems();
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stock Prices");
            writeHeader(wb, sheet);
            int r = 1;
            for (PriceListItemDto item : items) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(item.toolNo());
                row.createCell(1).setCellValue(item.minQty());
                row.createCell(2).setCellValue(item.price().doubleValue());
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] template() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Stock Prices");
            writeHeader(wb, sheet);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(XSSFWorkbook wb, Sheet sheet) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);

        Row header = sheet.createRow(0);
        String[] cols = {"tool_no", "min_qty", "price"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 3000);
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }
}
