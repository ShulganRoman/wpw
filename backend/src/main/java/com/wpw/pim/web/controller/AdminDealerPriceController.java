package com.wpw.pim.web.controller;

import com.wpw.pim.service.pricing.DealerPriceService;
import com.wpw.pim.web.dto.pricing.DealerPriceListDto;
import com.wpw.pim.web.dto.pricing.PriceImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dealers/{dealerId}/price-list")
@PreAuthorize("hasAuthority('MANAGE_PRICES')")
@RequiredArgsConstructor
@Tag(name = "Admin: Dealer Prices", description = "Dealer price list management: Excel import, export, deletion. Requires MANAGE_PRICES.")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "MANAGE_PRICES required")
})
public class AdminDealerPriceController {

    private final DealerPriceService dealerPriceService;

    @GetMapping
    @Operation(summary = "Get dealer price list")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Price list"),
        @ApiResponse(responseCode = "204", description = "No price list assigned"),
        @ApiResponse(responseCode = "404", description = "Dealer not found")
    })
    public ResponseEntity<DealerPriceListDto> get(@PathVariable UUID dealerId) {
        DealerPriceListDto dto = dealerPriceService.getForDealer(dealerId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import price list from Excel", description = "Excel file with columns: toolNo, minQty, price. Previous price list is replaced.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Import result"),
        @ApiResponse(responseCode = "400", description = "Invalid file or currency")
    })
    public PriceImportResult importPriceList(
        @PathVariable UUID dealerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam String currencyCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo
    ) throws IOException {
        return dealerPriceService.importPriceList(dealerId, file, currencyCode, validTo);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dealer price list")
    @ApiResponse(responseCode = "204", description = "Price list deleted")
    public void delete(@PathVariable UUID dealerId) {
        dealerPriceService.deletePriceList(dealerId);
    }

    @GetMapping("/export")
    @Operation(summary = "Export price list to Excel")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> export(@PathVariable UUID dealerId) throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"dealer-price-list.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(dealerPriceService.export(dealerId));
    }

    @GetMapping("/template")
    @Operation(summary = "Download Excel price list template")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> template() throws IOException {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"price-list-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(dealerPriceService.template());
    }
}
