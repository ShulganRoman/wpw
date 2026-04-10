package com.wpw.pim.service.product;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.domain.enums.PartRole;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductSparePart;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductSparePartRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.jsonld.JsonLdService;
import com.wpw.pim.web.dto.common.PagedResponse;
import com.wpw.pim.web.dto.product.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link ProductService}.
 * Покрывают получение фильтров, поиск товаров, создание, обновление, удаление,
 * spare parts, compatible tools, маппинг toSummary/toDetail.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

    @Mock private ProductRepository productRepo;
    @Mock private ProductTranslationRepository translationRepo;
    @Mock private ProductSparePartRepository sparePartRepo;
    @Mock private MediaFileRepository mediaFileRepo;
    @Mock private MediaFallbackService mediaFallback;
    @Mock private JsonLdService jsonLdService;
    @Mock private ProductGroupRepository productGroupRepo;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "mediaBasePath", "/tmp/test-media");
        ReflectionTestUtils.setField(productService, "mediaBaseUrl", "/media/products");
    }

    @Nested
    @DisplayName("getFilterOptions")
    class GetFilterOptions {

        @Test
        @DisplayName("возвращает карту фильтров")
        void getFilterOptions_returnsAllFilterKeys() {
            when(productRepo.findDistinctToolMaterials()).thenReturn(List.of("HSS", "Carbide"));
            when(productRepo.findDistinctWorkpieceMaterials()).thenReturn(List.of("Wood"));
            when(productRepo.findDistinctMachineTypes()).thenReturn(List.of("CNC"));
            when(productRepo.findDistinctMachineBrands()).thenReturn(List.of("Makita"));
            when(productRepo.findDistinctCuttingTypes()).thenReturn(List.of("Up-cut"));
            when(productRepo.findDistinctShankMm()).thenReturn(List.of(new java.math.BigDecimal("8.00")));

            Map<String, List<String>> options = productService.getFilterOptions();

            assertThat(options).containsKeys("toolMaterial", "workpieceMaterial", "machineType", "machineBrand", "cuttingType", "shankMm");
            assertThat(options.get("toolMaterial")).containsExactly("HSS", "Carbide");
            assertThat(options.get("shankMm")).containsExactly("8");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("возвращает пагинированный ответ")
        void findAll_returnsPagedResponse() {
            Product product = createProduct("TOOL-001");
            PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 48), 1);

            when(productRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("en"))).thenReturn(List.of());
            when(mediaFallback.getMediaForProduct(any())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);

            ProductFilter filter = new ProductFilter("en", null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, 1, 48);
            PagedResponse<ProductSummaryDto> response = productService.findAll(filter);

            assertThat(response.items()).hasSize(1);
            assertThat(response.total()).isEqualTo(1);
            assertThat(response.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAll с переводом и атрибутами")
        void findAll_withTranslationAndAttributes_mapsSummaryCorrectly() {
            Product product = createProductWithAttributes("TOOL-002");
            PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 48), 1);

            ProductTranslation translation = new ProductTranslation();
            translation.setId(new ProductTranslationId(product.getId(), "en"));
            translation.setProduct(product);
            translation.setName("Straight Bit");
            translation.setShortDescription("Short desc");

            when(productRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("en")))
                    .thenReturn(List.of(translation));
            when(mediaFallback.getMediaForProduct(any())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn("/thumb.webp");

            ProductFilter filter = new ProductFilter("en", null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, 1, 48);
            PagedResponse<ProductSummaryDto> response = productService.findAll(filter);

            assertThat(response.items()).hasSize(1);
            ProductSummaryDto dto = response.items().get(0);
            assertThat(dto.name()).isEqualTo("Straight Bit");
            assertThat(dto.shortDescription()).isEqualTo("Short desc");
            assertThat(dto.thumbnailUrl()).isEqualTo("/thumb.webp");
        }

        @Test
        @DisplayName("findAll для he locale помечает isRtl=true")
        void findAll_hebrewLocale_isRtl() {
            Product product = createProduct("TOOL-003");
            PageImpl<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 48), 1);

            when(productRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(translationRepo.findByProductIdsAndLocale(anyList(), eq("he"))).thenReturn(List.of());
            when(mediaFallback.getMediaForProduct(any())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);

            ProductFilter filter = new ProductFilter("he", null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, 1, 48);
            PagedResponse<ProductSummaryDto> response = productService.findAll(filter);

            assertThat(response.items().get(0).isRtl()).isTrue();
        }
    }

    @Nested
    @DisplayName("findByToolNo")
    class FindByToolNo {

        @Test
        @DisplayName("возвращает детали продукта")
        void findByToolNo_existingProduct_returnsDetail() {
            Product product = createProduct("TOOL-001");
            ProductTranslation translation = new ProductTranslation();
            translation.setId(new ProductTranslationId(product.getId(), "en"));
            translation.setProduct(product);
            translation.setName("Test Tool");

            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.of(translation));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            ProductDetailDto detail = productService.findByToolNo("TOOL-001", "en");

            assertThat(detail.toolNo()).isEqualTo("TOOL-001");
            assertThat(detail.name()).isEqualTo("Test Tool");
        }

        @Test
        @DisplayName("бросает NOT_FOUND если продукт не найден")
        void findByToolNo_notFound_throws404() {
            when(productRepo.findByToolNo("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findByToolNo("NONEXISTENT", "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("fallback на en перевод если запрошенная локаль не найдена")
        void findByToolNo_fallbackToEnTranslation() {
            Product product = createProduct("TOOL-001");
            ProductTranslation enTranslation = new ProductTranslation();
            enTranslation.setId(new ProductTranslationId(product.getId(), "en"));
            enTranslation.setProduct(product);
            enTranslation.setName("English Name");

            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "fr"))
                    .thenReturn(Optional.empty());
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.of(enTranslation));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("fr"))).thenReturn("{}");

            ProductDetailDto detail = productService.findByToolNo("TOOL-001", "fr");

            assertThat(detail.name()).isEqualTo("English Name");
        }

        @Test
        @DisplayName("возвращает detail с null translation - использует toolNo как имя")
        void findByToolNo_noTranslation_usesToolNoAsName() {
            Product product = createProduct("TOOL-001");

            when(productRepo.findByToolNo("TOOL-001")).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), isNull(), eq("en"))).thenReturn("{}");

            ProductDetailDto detail = productService.findByToolNo("TOOL-001", "en");

            assertThat(detail.name()).isEqualTo("TOOL-001");
        }

        @Test
        @DisplayName("возвращает detail с атрибутами, группой, категорией и секцией")
        void findByToolNo_withAttributesAndGroupHierarchy() {
            Product product = createProductWithAttributes("TOOL-ATT");
            Section section = new Section();
            section.setTranslations(Map.of("en", "Section1"));
            Category category = new Category();
            category.setSection(section);
            category.setTranslations(Map.of("en", "Category1"));
            ProductGroup group = new ProductGroup();
            group.setCategory(category);
            group.setTranslations(Map.of("en", "Group1"));
            product.setGroup(group);

            when(productRepo.findByToolNo("TOOL-ATT")).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), isNull(), eq("en"))).thenReturn("{}");

            ProductDetailDto detail = productService.findByToolNo("TOOL-ATT", "en");

            assertThat(detail.attributes()).isNotNull();
            assertThat(detail.attributes().dMm()).isEqualTo(new BigDecimal("12.5"));
            assertThat(detail.groupName()).isEqualTo("Group1");
            assertThat(detail.categoryName()).isEqualTo("Category1");
            assertThat(detail.sectionName()).isEqualTo("Section1");
        }

        @Test
        @DisplayName("возвращает mediaUrls из media списка")
        void findByToolNo_withMedia_returnsUrls() {
            Product product = createProduct("TOOL-MEDIA");
            MediaFile mf = new MediaFile();
            mf.setUrl("/media/products/TOOL-MEDIA/1.webp");

            when(productRepo.findByToolNo("TOOL-MEDIA")).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of(mf));
            when(mediaFallback.getThumbnail(anyList())).thenReturn("/media/products/TOOL-MEDIA/1.webp");
            when(jsonLdService.buildProductJsonLd(any(), isNull(), eq("en"))).thenReturn("{}");

            ProductDetailDto detail = productService.findByToolNo("TOOL-MEDIA", "en");

            assertThat(detail.mediaUrls()).containsExactly("/media/products/TOOL-MEDIA/1.webp");
            assertThat(detail.thumbnailUrl()).isEqualTo("/media/products/TOOL-MEDIA/1.webp");
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("обновляет продукт и возвращает detail")
        void updateProduct_existing_updatesAndReturns() {
            Product product = createProduct("TOOL-UPD");
            ProductUpdateDto dto = new ProductUpdateDto(
                    "ALT-001", ProductType.main, ProductStatus.active, true, (short) 5,
                    "Updated Name", "Short", "Long", "SEO Title", "SEO Desc",
                    "apps", null, Set.of("Carbide"), Set.of("Wood"), null, null, null);

            when(productRepo.findById(product.getId())).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.findByToolNo("TOOL-UPD")).thenReturn(Optional.of(product));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            ProductDetailDto result = productService.updateProduct(product.getId(), "en", dto);

            assertThat(result).isNotNull();
            verify(productRepo).save(any(Product.class));
            verify(translationRepo).save(any(ProductTranslation.class));
        }

        @Test
        @DisplayName("бросает NOT_FOUND если продукт не найден")
        void updateProduct_notFound_throws404() {
            UUID id = UUID.randomUUID();
            ProductUpdateDto dto = new ProductUpdateDto(null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(productRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(id, "en", dto))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("обновляет с attributes dto")
        void updateProduct_withAttributes_updatesAttributes() {
            Product product = createProduct("TOOL-ATTR");
            ProductAttributesDto attrsDto = new ProductAttributesDto(
                    new BigDecimal("12.5"), null, null, new BigDecimal("3.0"), null,
                    new BigDecimal("50.0"), null, null, null, null, new BigDecimal("8.0"),
                    "1/4\"", 2, 1, "up-cut", false, null, false, null, false, null, null,
                    null, null, null, null, null, null, null, null, null);
            ProductUpdateDto dto = new ProductUpdateDto(null, null, null, null, null,
                    null, null, null, null, null, null, attrsDto, null, null, null, null, null);

            when(productRepo.findById(product.getId())).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.findByToolNo("TOOL-ATTR")).thenReturn(Optional.of(product));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            productService.updateProduct(product.getId(), "en", dto);

            verify(productRepo).save(any(Product.class));
        }

        @Test
        @DisplayName("обновляет коллекции - пустой Set очищает, null не трогает")
        void updateProduct_collectionsUpdated() {
            Product product = createProduct("TOOL-COLL");
            product.getToolMaterials().add("OldMaterial");
            product.getMachineBrands().add("OldBrand");

            ProductUpdateDto dto = new ProductUpdateDto(null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    Set.of("NewMaterial"), null, null, Set.of(), null);

            when(productRepo.findById(product.getId())).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
                Product saved = inv.getArgument(0);
                assertThat(saved.getToolMaterials()).containsExactly("NewMaterial");
                assertThat(saved.getMachineBrands()).isEmpty();
                return saved;
            });
            when(productRepo.findByToolNo("TOOL-COLL")).thenReturn(Optional.of(product));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            productService.updateProduct(product.getId(), "en", dto);
        }

        @Test
        @DisplayName("обновляет существующий перевод вместо создания нового")
        void updateProduct_existingTranslation_updatesIt() {
            Product product = createProduct("TOOL-TR");
            ProductTranslation existing = new ProductTranslation();
            existing.setId(new ProductTranslationId(product.getId(), "en"));
            existing.setProduct(product);
            existing.setName("Old Name");

            ProductUpdateDto dto = new ProductUpdateDto(null, null, null, null, null,
                    "New Name", "New Short", null, null, null, null, null,
                    null, null, null, null, null);

            when(productRepo.findById(product.getId())).thenReturn(Optional.of(product));
            when(translationRepo.findByIdProductIdAndIdLocale(product.getId(), "en"))
                    .thenReturn(Optional.of(existing));
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.findByToolNo("TOOL-TR")).thenReturn(Optional.of(product));
            when(mediaFallback.getMediaForProduct(product.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            productService.updateProduct(product.getId(), "en", dto);

            verify(translationRepo).save(argThat(t -> {
                ProductTranslation pt = (ProductTranslation) t;
                return "New Name".equals(pt.getName()) && "New Short".equals(pt.getShortDescription());
            }));
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("удаляет продукт и файлы с диска")
        void deleteProduct_existing_deletesProductAndMedia() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-001");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.findByProductIds(List.of(productId))).thenReturn(List.of());

            productService.deleteProduct(productId);

            verify(productRepo).deleteById(productId);
        }

        @Test
        @DisplayName("бросает NOT_FOUND если продукт не найден")
        void deleteProduct_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(productRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(id))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("удаляет медиафайлы с диска включая thumbnails")
        void deleteProduct_withMediaAndThumbnails_deletesFiles() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-DEL");

            MediaFile mf = new MediaFile();
            mf.setUrl("/media/products/TOOL-DEL/1.webp");
            mf.setThumbnailUrl("/media/products/TOOL-DEL/1_thumb.webp");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.findByProductIds(List.of(productId))).thenReturn(List.of(mf));

            productService.deleteProduct(productId);

            verify(productRepo).deleteById(productId);
        }

        @Test
        @DisplayName("если thumbnail совпадает с url, удаляет файл один раз")
        void deleteProduct_sameThumbnailAsUrl_deletesOnce() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setToolNo("TOOL-SAME");

            MediaFile mf = new MediaFile();
            mf.setUrl("/media/products/TOOL-SAME/1.webp");
            mf.setThumbnailUrl("/media/products/TOOL-SAME/1.webp");

            when(productRepo.findById(productId)).thenReturn(Optional.of(product));
            when(mediaFileRepo.findByProductIds(List.of(productId))).thenReturn(List.of(mf));

            productService.deleteProduct(productId);

            verify(productRepo).deleteById(productId);
        }
    }

    @Nested
    @DisplayName("deleteProductsByGroupIds")
    class DeleteByGroupIds {

        @Test
        @DisplayName("не делает ничего при пустом списке groupIds")
        void deleteProductsByGroupIds_emptyList_doesNothing() {
            productService.deleteProductsByGroupIds(List.of());

            verify(productRepo, never()).findIdsByGroupIdIn(any());
        }

        @Test
        @DisplayName("не делает ничего при null groupIds")
        void deleteProductsByGroupIds_null_doesNothing() {
            productService.deleteProductsByGroupIds(null);

            verify(productRepo, never()).findIdsByGroupIdIn(any());
        }

        @Test
        @DisplayName("удаляет продукты по groupIds")
        void deleteProductsByGroupIds_validIds_deletesProducts() {
            UUID groupId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            when(productRepo.findIdsByGroupIdIn(List.of(groupId))).thenReturn(List.of(productId));
            when(mediaFileRepo.findByProductIds(List.of(productId))).thenReturn(List.of());

            productService.deleteProductsByGroupIds(List.of(groupId));

            verify(productRepo).deleteByIdIn(List.of(productId));
        }

        @Test
        @DisplayName("не удаляет если productIds пустой")
        void deleteProductsByGroupIds_noProducts_doesNotDelete() {
            UUID groupId = UUID.randomUUID();
            when(productRepo.findIdsByGroupIdIn(List.of(groupId))).thenReturn(List.of());

            productService.deleteProductsByGroupIds(List.of(groupId));

            verify(productRepo, never()).deleteByIdIn(any());
        }

        @Test
        @DisplayName("удаляет медиафайлы с диска включая thumbnail")
        void deleteProductsByGroupIds_withMedia_deletesMediaFiles() {
            UUID groupId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            MediaFile mf = new MediaFile();
            mf.setUrl("/media/products/TOOL/1.webp");
            mf.setThumbnailUrl("/media/products/TOOL/1_thumb.webp");

            when(productRepo.findIdsByGroupIdIn(List.of(groupId))).thenReturn(List.of(productId));
            when(mediaFileRepo.findByProductIds(List.of(productId))).thenReturn(List.of(mf));

            productService.deleteProductsByGroupIds(List.of(groupId));

            verify(productRepo).deleteByIdIn(List.of(productId));
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("бросает CONFLICT при дублировании toolNo")
        void createProduct_duplicateToolNo_throwsConflict() {
            ProductCreateDto dto = new ProductCreateDto("EXISTING", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);
            when(productRepo.existsByToolNo("EXISTING")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(dto))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("создаёт продукт с полным набором полей")
        void createProduct_fullDto_createsProductAndTranslation() {
            UUID groupId = UUID.randomUUID();
            ProductGroup group = new ProductGroup();
            group.setId(groupId);

            ProductCreateDto dto = new ProductCreateDto("NEW-001", groupId, "ALT-001",
                    ProductType.main, ProductStatus.active, true, (short) 10,
                    "New Tool", "Short", "Long", "SEO Title", "SEO Desc", "apps", null,
                    Set.of("Carbide"), Set.of("Wood"), Set.of("CNC"), Set.of("Makita"), Set.of("cut"));

            when(productRepo.existsByToolNo("NEW-001")).thenReturn(false);
            when(productGroupRepo.findById(groupId)).thenReturn(Optional.of(group));
            Product saved = createProduct("NEW-001");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.findByToolNo("NEW-001")).thenReturn(Optional.of(saved));
            when(translationRepo.findByIdProductIdAndIdLocale(saved.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(saved.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            ProductDetailDto result = productService.createProduct(dto);

            assertThat(result).isNotNull();
            verify(productRepo).save(any(Product.class));
            verify(translationRepo).save(any(ProductTranslation.class));
        }

        @Test
        @DisplayName("бросает NOT_FOUND если группа не найдена")
        void createProduct_groupNotFound_throws404() {
            UUID groupId = UUID.randomUUID();
            ProductCreateDto dto = new ProductCreateDto("NEW-002", groupId, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);

            when(productRepo.existsByToolNo("NEW-002")).thenReturn(false);
            when(productGroupRepo.findById(groupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(dto))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("создаёт продукт без имени - не сохраняет перевод")
        void createProduct_blankName_noTranslation() {
            ProductCreateDto dto = new ProductCreateDto("NO-NAME", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);

            when(productRepo.existsByToolNo("NO-NAME")).thenReturn(false);
            Product saved = createProduct("NO-NAME");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(productRepo.findByToolNo("NO-NAME")).thenReturn(Optional.of(saved));
            when(translationRepo.findByIdProductIdAndIdLocale(saved.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(saved.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            productService.createProduct(dto);

            verify(translationRepo, never()).save(any(ProductTranslation.class));
        }

        @Test
        @DisplayName("создаёт продукт с атрибутами")
        void createProduct_withAttributes_savesAttributes() {
            ProductAttributesDto attrsDto = new ProductAttributesDto(
                    new BigDecimal("12.5"), null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, false, null, false, null,
                    false, null, null, null, null, null, null, null, null, null, null, null);
            ProductCreateDto dto = new ProductCreateDto("WITH-ATTRS", null, null, null, null, null, null,
                    "Name", null, null, null, null, null, attrsDto,
                    null, null, null, null, null);

            when(productRepo.existsByToolNo("WITH-ATTRS")).thenReturn(false);
            Product saved = createProduct("WITH-ATTRS");
            when(productRepo.save(any(Product.class))).thenReturn(saved);
            when(translationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepo.findByToolNo("WITH-ATTRS")).thenReturn(Optional.of(saved));
            when(translationRepo.findByIdProductIdAndIdLocale(saved.getId(), "en"))
                    .thenReturn(Optional.empty());
            when(mediaFallback.getMediaForProduct(saved.getId())).thenReturn(List.of());
            when(mediaFallback.getThumbnail(anyList())).thenReturn(null);
            when(jsonLdService.buildProductJsonLd(any(), any(), eq("en"))).thenReturn("{}");

            productService.createProduct(dto);

            // save вызывается дважды: первый раз основной, второй после атрибутов
            verify(productRepo, times(2)).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("getSpareParts")
    class GetSpareParts {

        @Test
        @DisplayName("возвращает пустой список если нет запчастей")
        void getSpareParts_noSpareParts_returnsEmpty() {
            UUID productId = UUID.randomUUID();
            when(sparePartRepo.findByProductId(productId)).thenReturn(List.of());

            List<SparePartDto> result = productService.getSpareParts(productId, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("возвращает запчасти с переводом")
        void getSpareParts_withTranslation_returnsTranslatedName() {
            UUID productId = UUID.randomUUID();
            Product part = createProduct("PART-001");
            ProductTranslation partTranslation = new ProductTranslation();
            partTranslation.setId(new ProductTranslationId(part.getId(), "en"));
            partTranslation.setProduct(part);
            partTranslation.setName("Blade part");

            ProductSparePart sp = new ProductSparePart();
            Product parent = createProduct("PARENT-001");
            sp.setProduct(parent);
            sp.setPart(part);
            sp.setPartRole(PartRole.blade);

            when(sparePartRepo.findByProductId(productId)).thenReturn(List.of(sp));
            when(translationRepo.findByIdProductIdAndIdLocale(part.getId(), "en"))
                    .thenReturn(Optional.of(partTranslation));

            List<SparePartDto> result = productService.getSpareParts(productId, "en");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Blade part");
            assertThat(result.get(0).partRole()).isEqualTo(PartRole.blade);
        }

        @Test
        @DisplayName("использует toolNo если перевод не найден")
        void getSpareParts_noTranslation_usesToolNo() {
            UUID productId = UUID.randomUUID();
            Product part = createProduct("PART-002");

            ProductSparePart sp = new ProductSparePart();
            sp.setProduct(createProduct("PARENT"));
            sp.setPart(part);
            sp.setPartRole(PartRole.screw);

            when(sparePartRepo.findByProductId(productId)).thenReturn(List.of(sp));
            when(translationRepo.findByIdProductIdAndIdLocale(part.getId(), "en"))
                    .thenReturn(Optional.empty());

            List<SparePartDto> result = productService.getSpareParts(productId, "en");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("PART-002");
        }
    }

    @Nested
    @DisplayName("getCompatibleTools")
    class GetCompatibleTools {

        @Test
        @DisplayName("возвращает пустой список если нет совместимых инструментов")
        void getCompatibleTools_empty_returnsEmpty() {
            UUID partId = UUID.randomUUID();
            when(sparePartRepo.findByPartId(partId)).thenReturn(List.of());

            List<SparePartDto> result = productService.getCompatibleTools(partId, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("возвращает совместимые инструменты с переводом")
        void getCompatibleTools_withTranslation_returnsTranslatedName() {
            UUID partId = UUID.randomUUID();
            Product tool = createProduct("TOOL-COMPAT");
            ProductTranslation translation = new ProductTranslation();
            translation.setId(new ProductTranslationId(tool.getId(), "en"));
            translation.setProduct(tool);
            translation.setName("Compatible Tool");

            ProductSparePart sp = new ProductSparePart();
            sp.setProduct(tool);
            sp.setPart(createProduct("PART-X"));
            sp.setPartRole(PartRole.ball_bearing);

            when(sparePartRepo.findByPartId(partId)).thenReturn(List.of(sp));
            when(translationRepo.findByIdProductIdAndIdLocale(tool.getId(), "en"))
                    .thenReturn(Optional.of(translation));

            List<SparePartDto> result = productService.getCompatibleTools(partId, "en");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Compatible Tool");
            assertThat(result.get(0).toolNo()).isEqualTo("TOOL-COMPAT");
        }

        @Test
        @DisplayName("использует toolNo если перевод не найден")
        void getCompatibleTools_noTranslation_usesToolNo() {
            UUID partId = UUID.randomUUID();
            Product tool = createProduct("TOOL-NO-TR");

            ProductSparePart sp = new ProductSparePart();
            sp.setProduct(tool);
            sp.setPart(createProduct("PART-Y"));
            sp.setPartRole(PartRole.dust_shield);

            when(sparePartRepo.findByPartId(partId)).thenReturn(List.of(sp));
            when(translationRepo.findByIdProductIdAndIdLocale(tool.getId(), "en"))
                    .thenReturn(Optional.empty());

            List<SparePartDto> result = productService.getCompatibleTools(partId, "en");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("TOOL-NO-TR");
        }
    }

    // --- Helpers ---

    private Product createProduct(String toolNo) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setToolNo(toolNo);
        product.setOrderable(true);
        return product;
    }

    private Product createProductWithAttributes(String toolNo) {
        Product product = createProduct(toolNo);
        ProductAttributes attrs = new ProductAttributes();
        attrs.setProduct(product);
        attrs.setProductId(product.getId());
        attrs.setDMm(new BigDecimal("12.5"));
        attrs.setShankMm(new BigDecimal("8.0"));
        attrs.setFlutes((short) 2);
        product.setAttributes(attrs);
        return product;
    }
}
