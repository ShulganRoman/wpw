package com.wpw.pim.web.controller;

import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.service.pricing.StockPriceService;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin: Stock Prices", description = "Управление публичным прайс-листом (складские цены). Требует MANAGE_PRICES.")
public class AdminStockPriceController {

    private final StockPriceService stockPriceService;
    private final CurrencyRepository currencyRepository;

    @GetMapping("/currencies")
    @Operation(summary = "Список активных валют")
    public List<Currency> getCurrencies() {
        return currencyRepository.findByIsActiveTrueOrderByCode();
    }

    @GetMapping
    @Operation(summary = "Все позиции прайс-листа")
    public List<PriceListItemDto> list() {
        return stockPriceService.getItems();
    }

    @PutMapping
    @Operation(summary = "Создать или обновить позицию прайс-листа")
    public PriceListItemDto upsert(@Valid @RequestBody PriceListItemRequest request) {
        return stockPriceService.upsertItem(request);
    }

    @DeleteMapping("/{toolNo}/{minQty}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить позицию прайс-листа")
    public void delete(@PathVariable String toolNo, @PathVariable int minQty) {
        stockPriceService.deleteItem(toolNo, minQty);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Импортировать прайс-лист из Excel")
    public PriceImportResult importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return stockPriceService.importExcel(file);
    }

    @GetMapping("/export")
    @Operation(summary = "Экспортировать прайс-лист в Excel")
    public ResponseEntity<byte[]> export() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.export());
    }

    @GetMapping("/template")
    @Operation(summary = "Скачать шаблон прайс-листа Excel")
    public ResponseEntity<byte[]> template() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.template());
    }
}
