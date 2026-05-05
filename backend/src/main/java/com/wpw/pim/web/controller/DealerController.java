package com.wpw.pim.web.controller;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.dealer.DealerService;
import com.wpw.pim.service.dealer.SkuMappingService;
import com.wpw.pim.auth.repository.UserRepository;
import com.wpw.pim.web.dto.dealer.ChangePasswordRequest;
import com.wpw.pim.web.dto.dealer.PriceListDto;
import com.wpw.pim.web.dto.dealer.SkuMappingCreateRequest;
import com.wpw.pim.web.dto.dealer.SkuMappingDto;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dealer")
@PreAuthorize("hasRole('DEALER')")
@RequiredArgsConstructor
@Tag(name = "Dealer", description = "Dealer personal area: price list, SKU mapping, import. Requires DEALER role.")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "DEALER role required")
})
public class DealerController {

    private final DealerService dealerService;
    private final SkuMappingService skuMappingService;
    private final DealerRepository dealerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // --- price list ---

    @GetMapping("/price-list")
    @Operation(summary = "My price list", description = "Returns the personal price list of the current dealer.")
    @ApiResponse(responseCode = "200", description = "Personal price list")
    public PriceListDto getPriceList(@AuthenticationPrincipal UserDetails principal) {
        return dealerService.getPriceList(resolveDealer(principal));
    }

    // --- sku mapping CRUD ---

    @GetMapping("/sku-mapping")
    @Operation(summary = "My SKU mapping")
    @ApiResponse(responseCode = "200", description = "List of SKU mappings")
    public List<SkuMappingDto> getSkuMapping(@AuthenticationPrincipal UserDetails principal) {
        return dealerService.getSkuMapping(resolveDealer(principal).getId());
    }

    @PutMapping("/sku-mapping")
    @Operation(summary = "Create or update SKU mapping")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mapping saved"),
        @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public SkuMappingDto upsertSkuMapping(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody SkuMappingCreateRequest request
    ) {
        Dealer dealer = resolveDealer(principal);
        return dealerService.saveSkuMapping(dealer.getId(), request, dealer);
    }

    @DeleteMapping("/sku-mapping/{wpwSku}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete SKU mapping")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mapping deleted"),
        @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    public void deleteSkuMapping(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable String wpwSku
    ) {
        dealerService.deleteSkuMapping(resolveDealer(principal).getId(), wpwSku);
    }

    // --- import ---

    @PostMapping(value = "/sku-mapping/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate SKU mapping file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation report"),
        @ApiResponse(responseCode = "400", description = "Invalid file")
    })
    public SkuMappingService.ValidationReport validate(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        resolveDealer(principal); // auth check
        return skuMappingService.validate(file);
    }

    @PostMapping(value = "/sku-mapping/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import SKU mapping from Excel")
    @ApiResponse(responseCode = "200", description = "Import result")
    public SkuMappingService.SkuMappingImportResult execute(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "false") boolean skipGhosts
    ) throws IOException {
        return skuMappingService.execute(resolveDealer(principal).getId(), file, skipGhosts);
    }

    @GetMapping("/sku-mapping/export")
    @Operation(summary = "Export my SKU mapping to Excel")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal UserDetails principal) throws IOException {
        byte[] bytes = skuMappingService.export(resolveDealer(principal).getId());
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"my-sku-mapping.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/sku-mapping/template")
    @Operation(summary = "Download SKU mapping template")
    @ApiResponse(responseCode = "200", description = "Excel file")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = skuMappingService.template();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"sku-mapping-template.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    // --- change password ---

    @PostMapping("/change-password")
    @Operation(summary = "Change own password")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password changed"),
        @ApiResponse(responseCode = "400", description = "Wrong current password or validation error")
    })
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        var user = userRepository.findByUsernameWithRole(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // --- helpers ---

    private Dealer resolveDealer(UserDetails principal) {
        if (principal instanceof DealerPrincipal dp) return dp.getDealer();
        return dealerRepository.findByUserUsername(principal.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Dealer profile not found for: " + principal.getUsername()));
    }
}
