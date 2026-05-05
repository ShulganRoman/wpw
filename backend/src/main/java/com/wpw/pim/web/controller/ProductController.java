package com.wpw.pim.web.controller;

import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.service.dealer.DealerSkuResolverService;
import com.wpw.pim.service.media.ProductMediaService;
import com.wpw.pim.service.pricing.PriceResolverService;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.media.MediaImageDto;
import com.wpw.pim.web.dto.product.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog: filtering, product details, image management")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
})
public class ProductController {

    private final ProductService productService;
    private final ProductMediaService productMediaService;
    private final PriceResolverService priceResolverService;
    private final DealerSkuResolverService dealerSkuResolverService;
    private final DealerRepository dealerRepository;

    private UUID extractDealerId(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof DealerPrincipal dp) return dp.getDealer().getId();
        boolean isDealer = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_DEALER".equals(a.getAuthority()));
        if (isDealer) {
            return dealerRepository.findByUserUsername(auth.getName())
                .map(d -> d.getId())
                .orElse(null);
        }
        return null;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        // dealers have ROLE_DEALER — they are never treated as admins for image management
        return auth.getAuthorities().stream()
            .noneMatch(a -> "ROLE_DEALER".equals(a.getAuthority()));
    }

    @GetMapping("/filter-options")
    @Operation(summary = "Filter options", description = "Returns all available values for filters (materials, machine types, etc.).")
    @ApiResponse(responseCode = "200", description = "Available filter values")
    public Map<String, List<String>> getFilterOptions() {
        return productService.getFilterOptions();
    }

    @GetMapping
    @Operation(summary = "Product list with filters", description = "Filtering by catalog, attributes, price (for authenticated users). Supports pagination.")
    @ApiResponse(responseCode = "200", description = "Paginated product list")
    public PagedResponse<ProductSummaryDto> list(
        @RequestParam(defaultValue = "en") String locale,
        @RequestParam(required = false) UUID sectionId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID groupId,
        @RequestParam(required = false) String operation,
        @RequestParam(required = false) List<String> toolMaterial,
        @RequestParam(required = false) List<String> workpieceMaterial,
        @RequestParam(required = false) List<String> machineType,
        @RequestParam(required = false) List<String> machineBrand,
        @RequestParam(required = false) List<String> cuttingType,
        @RequestParam(required = false) BigDecimal dMmMin,
        @RequestParam(required = false) BigDecimal dMmMax,
        @RequestParam(required = false) BigDecimal shankMm,
        @RequestParam(required = false) Boolean hasBallBearing,
        @RequestParam(required = false) String productType,
        @RequestParam(required = false) Boolean inStock,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "48") int perPage,
        @RequestParam(required = false) BigDecimal priceMin,
        @RequestParam(required = false) BigDecimal priceMax,
        Authentication auth
    ) {
        UUID priceListId = priceResolverService.resolvePriceListId(auth);
        ProductFilter filter = new ProductFilter(locale, sectionId, categoryId, groupId, operation, toolMaterial, workpieceMaterial,
            machineType, machineBrand, cuttingType, dMmMin, dMmMax, shankMm, hasBallBearing,
            productType, inStock, page, perPage, priceMin, priceMax, priceListId);
        PagedResponse<ProductSummaryDto> result = productService.findAll(filter);
        if (auth == null || !auth.isAuthenticated() || result.items().isEmpty()) return result;

        List<UUID> ids = result.items().stream().map(ProductSummaryDto::id).toList();
        List<String> toolNos = result.items().stream().map(ProductSummaryDto::toolNo).toList();
        Map<UUID, PriceInfoDto> prices = priceResolverService.resolveBatch(ids, auth);
        Map<String, String> dealerSkus = dealerSkuResolverService.resolveBatch(toolNos, auth);

        if (prices.isEmpty() && dealerSkus.isEmpty()) return result;

        List<ProductSummaryDto> enriched = result.items().stream()
            .map(p -> new ProductSummaryDto(
                p.id(), p.toolNo(), p.altToolNo(), p.name(), p.shortDescription(),
                p.productType(), p.status(), p.isOrderable(), p.dMm(), p.shankMm(),
                p.cuttingType(), p.stockStatus(), p.thumbnailUrl(), p.locale(), p.isRtl(),
                prices.getOrDefault(p.id(), p.price()),
                dealerSkus.getOrDefault(p.toolNo(), p.dealerSku())))
            .toList();
        return new PagedResponse<>(enriched, result.total(), result.page(), result.perPage(), result.totalPages());
    }

    @GetMapping("/{toolNo}")
    @Operation(summary = "Product details", description = "Detailed product information by SKU. Dealer sees their own SKU and price.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product details"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ProductDetailDto getByToolNo(
        @PathVariable String toolNo,
        @RequestParam(defaultValue = "en") String locale,
        Authentication auth
    ) {
        ProductDetailDto dto = productService.findByToolNo(toolNo, locale);
        PriceInfoDto price = priceResolverService.resolve(toolNo, auth);
        String dealerSku = dealerSkuResolverService.resolve(toolNo, auth);
        if (price == null && dealerSku == null) return dto;
        return new ProductDetailDto(
            dto.id(), dto.toolNo(), dto.altToolNo(), dto.productType(), dto.status(),
            dto.isOrderable(), dto.catalogPage(), dto.name(), dto.shortDescription(),
            dto.longDescription(), dto.seoTitle(), dto.seoDescription(), dto.applications(),
            dto.aiGenerated(), dto.locale(), dto.isRtl(), dto.attributes(),
            dto.toolMaterials(), dto.workpieceMaterials(), dto.machineTypes(),
            dto.machineBrands(), dto.operationCodes(), dto.mediaUrls(), dto.thumbnailUrl(),
            dto.jsonLd(), dto.groupName(), dto.categoryName(), dto.sectionName(),
            price != null ? price : dto.price(), dealerSku
        );
    }

    @GetMapping("/{id}/spare-parts")
    @Operation(summary = "Spare parts", description = "List of spare parts for the specified product.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List returned"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public List<SparePartDto> getSpareParts(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "en") String locale
    ) {
        return productService.getSpareParts(id, locale);
    }

    @GetMapping("/{id}/compatible-tools")
    @Operation(summary = "Compatible tools", description = "List of tools compatible with the specified product.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List returned"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public List<SparePartDto> getCompatibleTools(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "en") String locale
    ) {
        return productService.getCompatibleTools(id, locale);
    }

    /**
     * Создаёт новый товар.
     *
     * @param dto данные нового продукта (toolNo обязателен)
     * @return созданный {@link ProductDetailDto} со статусом 201
     */
    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS')")
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product. Requires MODIFY_PRODUCTS.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public ResponseEntity<ProductDetailDto> create(@RequestBody ProductCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto));
    }

    /**
     * Обновляет товар по его идентификатору.
     *
     * @param id     идентификатор продукта
     * @param locale языковая локаль для обновления перевода
     * @param dto    данные для обновления
     * @return обновлённый {@link ProductDetailDto}
     */
    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS')")
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates product by ID and locale. Requires MODIFY_PRODUCTS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "400", description = "Invalid data"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ProductDetailDto update(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "en") String locale,
        @RequestBody ProductUpdateDto dto
    ) {
        return productService.updateProduct(id, locale, dto);
    }

    /**
     * Удаляет товар по его идентификатору вместе с файлами изображений на диске.
     *
     * @param id идентификатор продукта
     * @return 204 No Content
     */
    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes product and all associated images from disk. Requires MODIFY_PRODUCTS.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ========================= Управление изображениями =========================

    /**
     * Возвращает список изображений товара.
     *
     * @param id идентификатор продукта
     * @return список {@link MediaImageDto}
     */
    @GetMapping("/{id}/images")
    @Operation(summary = "Product images", description = "List of all product media files sorted by sort_order.")
    @ApiResponse(responseCode = "200", description = "List of media files")
    public List<MediaImageDto> getImages(@PathVariable UUID id, Authentication auth) {
        return productMediaService.getImages(id, extractDealerId(auth), isAdmin(auth));
    }

    /**
     * Добавляет изображения к товару (конвертация в WebP).
     *
     * @param id    идентификатор продукта
     * @param files массив загружаемых файлов изображений
     * @return обновлённый список {@link MediaImageDto}
     */
    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS') or hasRole('DEALER')")
    @PostMapping("/{id}/images")
    @Operation(summary = "Add images", description = "Uploads and converts images to WebP. Requires MODIFY_PRODUCTS or DEALER role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Images uploaded and converted to WebP"),
        @ApiResponse(responseCode = "400", description = "Invalid file format")
    })
    public List<MediaImageDto> addImages(
        @PathVariable UUID id,
        @RequestParam("files") MultipartFile[] files,
        Authentication auth
    ) {
        return productMediaService.addImages(id, files, extractDealerId(auth), isAdmin(auth));
    }

    /**
     * Удаляет изображение товара.
     *
     * @param id      идентификатор продукта
     * @param imageId идентификатор медиафайла
     * @return обновлённый список {@link MediaImageDto}
     */
    @PreAuthorize("hasAuthority('MODIFY_PRODUCTS') or hasRole('DEALER')")
    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete product image", description = "Deletes media file from disk and DB. Requires MODIFY_PRODUCTS or DEALER role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image deleted, returns updated list"),
        @ApiResponse(responseCode = "404", description = "Product or image not found")
    })
    public List<MediaImageDto> deleteImage(
        @PathVariable UUID id,
        @PathVariable UUID imageId,
        Authentication auth
    ) {
        return productMediaService.deleteImage(id, imageId, extractDealerId(auth), isAdmin(auth));
    }
}
