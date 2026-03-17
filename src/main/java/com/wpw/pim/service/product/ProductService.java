package com.wpw.pim.service.product;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductSparePart;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductSparePartRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.jsonld.JsonLdService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
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
