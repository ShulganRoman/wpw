package com.wpw.pim.web.controller;

import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.service.pricing.StockPriceService;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/price/stock")
@PreAuthorize("hasAuthority('MANAGE_PRICES')")
@RequiredArgsConstructor
public class AdminStockPriceController {

    private final StockPriceService stockPriceService;
    private final CurrencyRepository currencyRepository;

    @GetMapping("/currencies")
    public List<Currency> getCurrencies() {
        return currencyRepository.findByIsActiveTrueOrderByCode();
    }

    @GetMapping
    public List<PriceListItemDto> list() {
        return stockPriceService.getItems();
    }

    @PutMapping
    public PriceListItemDto upsert(@Valid @RequestBody PriceListItemRequest request) {
        return stockPriceService.upsertItem(request);
    }

    @DeleteMapping("/{toolNo}/{minQty}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String toolNo, @PathVariable int minQty) {
        stockPriceService.deleteItem(toolNo, minQty);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PriceImportResult importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return stockPriceService.importExcel(file);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.export());
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.template());
    }
}
