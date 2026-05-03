package com.wpw.pim.web.controller;

import com.wpw.pim.domain.pricing.Currency;
import com.wpw.pim.repository.pricing.CurrencyRepository;
import com.wpw.pim.service.pricing.StockPriceService;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import com.wpw.pim.web.dto.pricing.PriceListItemDto;
import com.wpw.pim.web.dto.pricing.PriceListItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Admin: Stock Prices", description = "Public price list management (stock prices). Requires MANAGE_PRICES.")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "MANAGE_PRICES required")
})
public class AdminStockPriceController {

    private final StockPriceService stockPriceService;
    private final CurrencyRepository currencyRepository;

    @GetMapping("/currencies")
    @Operation(summary = "List active currencies")
    @ApiResponse(responseCode = "200", description = "Active currencies")
    public List<Currency> getCurrencies() {
        return currencyRepository.findByIsActiveTrueOrderByCode();
    }

    @GetMapping
    @Operation(summary = "All price list entries")
    @ApiResponse(responseCode = "200", description = "All price list entries")
    public List<PriceListItemDto> list() {
        return stockPriceService.getItems();
    }

    @PutMapping
    @Operation(summary = "Create or update price list entry")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entry saved"),
        @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public PriceListItemDto upsert(@Valid @RequestBody PriceListItemRequest request) {
        return stockPriceService.upsertItem(request);
    }

    @DeleteMapping("/{toolNo}/{minQty}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete price list entry")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entry deleted"),
        @ApiResponse(responseCode = "404", description = "Entry not found")
    })
    public void delete(@PathVariable String toolNo, @PathVariable int minQty) {
        stockPriceService.deleteItem(toolNo, minQty);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import price list from Excel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import result"),
        @ApiResponse(responseCode = "400", description = "Invalid file")
    })
    public PriceImportResult importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return stockPriceService.importExcel(file);
    }

    @GetMapping("/export")
    @Operation(summary = "Export price list to Excel")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> export() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.export());
    }

    @GetMapping("/template")
    @Operation(summary = "Download Excel price list template")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> template() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"stock-prices-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(stockPriceService.template());
    }
}
