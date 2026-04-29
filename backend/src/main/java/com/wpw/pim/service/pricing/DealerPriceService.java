package com.wpw.pim.service.pricing;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.domain.pricing.PriceListItem;
import com.wpw.pim.domain.pricing.PriceListItemId;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.repository.pricing.PriceListItemRepository;
import com.wpw.pim.repository.pricing.PriceListRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.web.dto.pricing.DealerPriceListDto;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DealerPriceService {

    private final DealerRepository dealerRepo;
    private final PriceListRepository priceListRepo;
    private final PriceListItemRepository itemRepo;
    private final CurrencyRepository currencyRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public DealerPriceListDto getForDealer(UUID dealerId) {
        Dealer dealer = dealerRepo.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PriceList pl = dealer.getPriceList();
        if (pl == null) return null;

        boolean expired = pl.getValidTo() != null && pl.getValidTo().isBefore(LocalDate.now());
        List<PriceListItemDto> items = itemRepo.findByPriceListIdOrderByIdMinQtyAsc(pl.getId()).stream()
            .map(i -> new PriceListItemDto(i.getProduct().getToolNo(), i.getId().getMinQty(), i.getPrice()))
            .toList();

        return new DealerPriceListDto(
            pl.getCurrency().getCode(), pl.getCurrency().getSymbol(),
            pl.getValidFrom(), pl.getValidTo(), expired, items
        );
    }

    @Transactional
    public PriceImportResult importPriceList(UUID dealerId, MultipartFile file,
                                              String currencyCode, LocalDate validTo) throws IOException {
        Dealer dealer = dealerRepo.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Currency currency = currencyRepo.findById(currencyCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency: " + currencyCode));

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
                    productRepo.findByToolNo(toolNo).ifPresentOrElse(product -> {
                        PriceListItem item = new PriceListItem();
                        // id will be set after price list is created
                        item.setProduct(product);
                        item.setPrice(price);
                        // store minQty temporarily in a dummy id
                        item.setId(new PriceListItemId(null, product.getId(), minQty));
                        toSave.add(item);
                    }, () -> errors.add("Row " + (rowNum + 1) + ": product not found: " + toolNo));
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        // Create or reuse price list
        PriceList pl = dealer.getPriceList();
        if (pl == null) {
            pl = new PriceList();
            pl.setType("dealer");
        }
        pl.setName("Dealer-" + dealerId);
        pl.setCurrency(currency);
        pl.setValidFrom(LocalDate.now());
        pl.setValidTo(validTo);
        pl = priceListRepo.save(pl);

        // Replace all items
        itemRepo.deleteByPriceListId(pl.getId());

        final UUID plId = pl.getId();
        for (PriceListItem item : toSave) {
            item.setId(new PriceListItemId(plId, item.getId().getProductId(), item.getId().getMinQty()));
            item.setPriceList(pl);
        }
        itemRepo.saveAll(toSave);

        // Link dealer → price list
        dealer.setPriceList(pl);
        dealerRepo.save(dealer);

        return new PriceImportResult(toSave.size(), errors.size(), errors);
    }

    @Transactional
    public void deletePriceList(UUID dealerId) {
        Dealer dealer = dealerRepo.findById(dealerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PriceList pl = dealer.getPriceList();
        if (pl == null) return;
        dealer.setPriceList(null);
        dealerRepo.save(dealer);
        itemRepo.deleteByPriceListId(pl.getId());
        priceListRepo.delete(pl);
    }

    public byte[] export(UUID dealerId) throws IOException {
        DealerPriceListDto dto = getForDealer(dealerId);
        if (dto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No price list for dealer");

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Price List");
            writeHeader(wb, sheet, dto.currencyCode());
            int r = 1;
            for (PriceListItemDto item : dto.items()) {
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
            Sheet sheet = wb.createSheet("Price List");
            writeHeader(wb, sheet, "USD");
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(XSSFWorkbook wb, Sheet sheet, String currencyCode) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);

        Row header = sheet.createRow(0);
        String[] cols = {"tool_no", "min_qty", "price (" + currencyCode + ")"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
        }
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 4000);
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
