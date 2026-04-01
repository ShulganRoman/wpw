package com.wpw.pim.service.product;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductSparePart;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductSparePartRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.jsonld.JsonLdService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductTranslationRepository translationRepo;
    private final ProductSparePartRepository sparePartRepo;
    private final MediaFallbackService mediaFallback;
    private final JsonLdService jsonLdService;

    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryDto> findAll(ProductFilter filter) {
        PageRequest pageable = PageRequest.of(filter.page() - 1, filter.perPage());
        Page<Product> page = productRepo.findAll(new ProductFilterSpec(filter), pageable);

        List<UUID> ids = page.map(Product::getId).toList();
        Map<UUID, ProductTranslation> translations = translationRepo
            .findByProductIdsAndLocale(ids, filter.locale())
            .stream().collect(Collectors.toMap(pt -> pt.getId().getProductId(), pt -> pt));

        List<ProductSummaryDto> items = page.getContent().stream()
            .map(p -> toSummary(p, translations.get(p.getId()), filter.locale()))
            .toList();

        return PagedResponse.of(items, page.getTotalElements(), filter.page(), filter.perPage());
    }

    @Transactional(readOnly = true)
    public ProductDetailDto findByToolNo(String toolNo, String locale) {
        Product product = productRepo.findByToolNo(toolNo)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found: " + toolNo));

        ProductTranslation translation = translationRepo
            .findByIdProductIdAndIdLocale(product.getId(), locale)
            .or(() -> translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
            .orElse(null);

        List<MediaFile> media = mediaFallback.getMediaForProduct(product.getId());
        String jsonLd = jsonLdService.buildProductJsonLd(product, translation, locale);

        return toDetail(product, translation, media, locale, jsonLd);
    }

    @Transactional(readOnly = true)
    public List<SparePartDto> getSpareParts(UUID productId, String locale) {
        return sparePartRepo.findByProductId(productId).stream()
            .map(sp -> {
                Product part = sp.getPart();
                ProductTranslation t = translationRepo
                    .findByIdProductIdAndIdLocale(part.getId(), locale).orElse(null);
                String name = t != null ? t.getName() : part.getToolNo();
                return new SparePartDto(part.getId(), part.getToolNo(), name, sp.getPartRole());
            }).toList();
    }

    @Transactional(readOnly = true)
    public List<SparePartDto> getCompatibleTools(UUID partId, String locale) {
        return sparePartRepo.findByPartId(partId).stream()
            .map(sp -> {
                Product product = sp.getProduct();
                ProductTranslation t = translationRepo
                    .findByIdProductIdAndIdLocale(product.getId(), locale).orElse(null);
                String name = t != null ? t.getName() : product.getToolNo();
                return new SparePartDto(product.getId(), product.getToolNo(), name, sp.getPartRole());
            }).toList();
    }

    /**
     * Обновляет товар по его идентификатору.
     * <p>
     * Обновляет основные поля продукта, перевод для указанной locale,
     * технические атрибуты и коллекции материалов/типов.
     * Для коллекций: {@code null} — не менять, пустой {@link Set} — очистить.
     * </p>
     *
     * @param id     идентификатор продукта
     * @param locale языковая локаль для обновления перевода
     * @param dto    данные для обновления
     * @return обновлённый {@link ProductDetailDto}
     * @throws ResponseStatusException 404 если продукт не найден
     */
    @Transactional
    public ProductDetailDto updateProduct(UUID id, String locale, ProductUpdateDto dto) {
        Product product = productRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found: " + id));

        // Обновление основных полей Product
        if (dto.altToolNo() != null) product.setAltToolNo(dto.altToolNo());
        if (dto.productType() != null) product.setProductType(dto.productType());
        if (dto.status() != null) product.setStatus(dto.status());
        if (dto.isOrderable() != null) product.setOrderable(dto.isOrderable());
        if (dto.catalogPage() != null) product.setCatalogPage(dto.catalogPage());

        // Обновление или создание перевода для указанной locale
        updateTranslation(product, locale, dto);

        // Обновление или создание атрибутов
        if (dto.attributes() != null) {
            updateAttributes(product, dto.attributes());
        }

        // Обновление коллекций (null = не менять, пустой Set = очистить)
        updateCollection(product.getToolMaterials(), dto.toolMaterials());
        updateCollection(product.getWorkpieceMaterials(), dto.workpieceMaterials());
        updateCollection(product.getMachineTypes(), dto.machineTypes());
        updateCollection(product.getMachineBrands(), dto.machineBrands());
        updateCollection(product.getOperationCodes(), dto.operationCodes());

        productRepo.save(product);
        log.info("Product updated: id={}, toolNo={}, locale={}", id, product.getToolNo(), locale);

        return findByToolNo(product.getToolNo(), locale);
    }

    /**
     * Обновляет или создаёт перевод продукта для указанной locale.
     */
    private void updateTranslation(Product product, String locale, ProductUpdateDto dto) {
        ProductTranslation translation = translationRepo
            .findByIdProductIdAndIdLocale(product.getId(), locale)
            .orElseGet(() -> {
                ProductTranslation t = new ProductTranslation();
                t.setId(new ProductTranslationId(product.getId(), locale));
                t.setProduct(product);
                t.setName(product.getToolNo()); // default name
                return t;
            });

        if (dto.name() != null) translation.setName(dto.name());
        if (dto.shortDescription() != null) translation.setShortDescription(dto.shortDescription());
        if (dto.longDescription() != null) translation.setLongDescription(dto.longDescription());
        if (dto.seoTitle() != null) translation.setSeoTitle(dto.seoTitle());
        if (dto.seoDescription() != null) translation.setSeoDescription(dto.seoDescription());
        if (dto.applications() != null) translation.setApplications(dto.applications());

        translationRepo.save(translation);
    }

    /**
     * Обновляет или создаёт технические атрибуты продукта.
     */
    private void updateAttributes(Product product, ProductAttributesDto a) {
        ProductAttributes attrs = product.getAttributes();
        if (attrs == null) {
            attrs = new ProductAttributes();
            attrs.setProduct(product);
            attrs.setProductId(product.getId());
            product.setAttributes(attrs);
        }

        attrs.setDMm(a.dMm());
        attrs.setD1Mm(a.d1Mm());
        attrs.setD2Mm(a.d2Mm());
        attrs.setBMm(a.bMm());
        attrs.setB1Mm(a.b1Mm());
        attrs.setLMm(a.lMm());
        attrs.setL1Mm(a.l1Mm());
        attrs.setRMm(a.rMm());
        attrs.setAMm(a.aMm());
        attrs.setAngleDeg(a.angleDeg());
        attrs.setShankMm(a.shankMm());
        attrs.setShankInch(a.shankInch());
        attrs.setFlutes(a.flutes() != null ? a.flutes().shortValue() : null);
        attrs.setBladeNo(a.bladeNo() != null ? a.bladeNo().shortValue() : null);
        attrs.setCuttingType(a.cuttingType());
        attrs.setHasBallBearing(a.hasBallBearing());
        attrs.setBallBearingCode(a.ballBearingCode());
        attrs.setHasRetainer(a.hasRetainer());
        attrs.setRetainerCode(a.retainerCode());
        attrs.setCanResharpen(a.canResharpen());
        attrs.setRotationDirection(a.rotationDirection());
        attrs.setBoreType(a.boreType());
        attrs.setEan13(a.ean13());
        attrs.setUpc12(a.upc12());
        attrs.setHsCode(a.hsCode());
        attrs.setCountryOfOrigin(a.countryOfOrigin());
        attrs.setWeightG(a.weightG());
        attrs.setPkgQty(a.pkgQty() != null ? a.pkgQty().shortValue() : null);
        attrs.setCartonQty(a.cartonQty() != null ? a.cartonQty().shortValue() : null);
        attrs.setStockStatus(a.stockStatus());
        attrs.setStockQty(a.stockQty());
    }

    /**
     * Обновляет коллекцию элементов: null — не менять, непустой/пустой Set — заменить.
     */
    private void updateCollection(Set<String> target, Set<String> source) {
        if (source == null) return;
        target.clear();
        target.addAll(source);
    }

    private ProductSummaryDto toSummary(Product p, ProductTranslation t, String locale) {
        ProductAttributes a = p.getAttributes();
        List<MediaFile> media = mediaFallback.getMediaForProduct(p.getId());
        return new ProductSummaryDto(
            p.getId(), p.getToolNo(), p.getAltToolNo(),
            t != null ? t.getName() : p.getToolNo(),
            t != null ? t.getShortDescription() : null,
            p.getProductType(), p.getStatus(), p.isOrderable(),
            a != null ? a.getDMm() : null,
            a != null ? a.getShankMm() : null,
            a != null ? a.getCuttingType() : null,
            a != null ? a.getStockStatus() : null,
            mediaFallback.getThumbnail(media),
            locale, "he".equals(locale)
        );
    }

    private ProductDetailDto toDetail(Product p, ProductTranslation t, List<MediaFile> media, String locale, String jsonLd) {
        ProductAttributes a = p.getAttributes();
        ProductAttributesDto attrsDto = a == null ? null : new ProductAttributesDto(
            a.getDMm(), a.getD1Mm(), a.getD2Mm(), a.getBMm(), a.getB1Mm(),
            a.getLMm(), a.getL1Mm(), a.getRMm(), a.getAMm(), a.getAngleDeg(),
            a.getShankMm(), a.getShankInch(),
            a.getFlutes() != null ? a.getFlutes().intValue() : null,
            a.getBladeNo() != null ? a.getBladeNo().intValue() : null,
            a.getCuttingType(),
            a.isHasBallBearing(), a.getBallBearingCode(),
            a.isHasRetainer(), a.getRetainerCode(),
            a.isCanResharpen(),
            a.getRotationDirection(), a.getBoreType(),
            a.getEan13(), a.getUpc12(), a.getHsCode(), a.getCountryOfOrigin(),
            a.getWeightG(),
            a.getPkgQty() != null ? a.getPkgQty().intValue() : null,
            a.getCartonQty() != null ? a.getCartonQty().intValue() : null,
            a.getStockStatus(), a.getStockQty()
        );

        String groupName = p.getGroup() != null ? p.getGroup().getTranslations().getOrDefault(locale, "") : null;
        String categoryName = p.getGroup() != null && p.getGroup().getCategory() != null
            ? p.getGroup().getCategory().getTranslations().getOrDefault(locale, "") : null;
        String sectionName = p.getGroup() != null && p.getGroup().getCategory() != null
            && p.getGroup().getCategory().getSection() != null
            ? p.getGroup().getCategory().getSection().getTranslations().getOrDefault(locale, "") : null;

        return new ProductDetailDto(
            p.getId(), p.getToolNo(), p.getAltToolNo(),
            p.getProductType(), p.getStatus(), p.isOrderable(), p.getCatalogPage(),
            t != null ? t.getName() : p.getToolNo(),
            t != null ? t.getShortDescription() : null,
            t != null ? t.getLongDescription() : null,
            t != null ? t.getSeoTitle() : null,
            t != null ? t.getSeoDescription() : null,
            t != null ? t.getApplications() : null,
            t != null && t.isAiGenerated(),
            locale, "he".equals(locale),
            attrsDto,
            new java.util.HashSet<>(p.getToolMaterials()), new java.util.HashSet<>(p.getWorkpieceMaterials()), new java.util.HashSet<>(p.getMachineTypes()), new java.util.HashSet<>(p.getMachineBrands()), new java.util.HashSet<>(p.getOperationCodes()),
            media.stream().map(MediaFile::getUrl).toList(),
            mediaFallback.getThumbnail(media),
            jsonLd,
            groupName, categoryName, sectionName
        );
    }
}
