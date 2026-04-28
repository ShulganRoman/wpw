package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.dealer.DealerService;
import com.wpw.pim.service.dealer.SkuMappingService;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dealer")
@PreAuthorize("hasRole('DEALER')")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;
    private final SkuMappingService skuMappingService;
    private final DealerRepository dealerRepository;

    // --- price list ---

    @GetMapping("/price-list")
    public PriceListDto getPriceList(@AuthenticationPrincipal UserDetails principal) {
        return dealerService.getPriceList(resolveDealer(principal));
    }

    // --- sku mapping CRUD ---

    @GetMapping("/sku-mapping")
    public List<SkuMappingDto> getSkuMapping(@AuthenticationPrincipal UserDetails principal) {
        return dealerService.getSkuMapping(resolveDealer(principal).getId());
    }

    @PutMapping("/sku-mapping")
    public SkuMappingDto upsertSkuMapping(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody SkuMappingCreateRequest request
    ) {
        Dealer dealer = resolveDealer(principal);
        return dealerService.saveSkuMapping(dealer.getId(), request, dealer);
    }

    @DeleteMapping("/sku-mapping/{wpwSku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkuMapping(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable String wpwSku
    ) {
        dealerService.deleteSkuMapping(resolveDealer(principal).getId(), wpwSku);
    }

    // --- import ---

    @PostMapping(value = "/sku-mapping/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkuMappingService.ValidationReport validate(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        resolveDealer(principal); // auth check
        return skuMappingService.validate(file);
    }

    @PostMapping(value = "/sku-mapping/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SkuMappingService.SkuMappingImportResult execute(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean skipGhosts
    ) throws IOException {
        return skuMappingService.execute(resolveDealer(principal).getId(), file, skipGhosts);
    }

    @GetMapping("/sku-mapping/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal UserDetails principal) throws IOException {
        byte[] bytes = skuMappingService.export(resolveDealer(principal).getId());
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"my-sku-mapping.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/sku-mapping/template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = skuMappingService.template();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    // --- helpers ---

    private Dealer resolveDealer(UserDetails principal) {
        if (principal instanceof DealerPrincipal dp) return dp.getDealer();
        return dealerRepository.findByUserUsername(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Dealer profile not found for: " + principal.getUsername()));
    }
}
