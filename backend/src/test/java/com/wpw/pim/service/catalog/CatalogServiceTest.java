package com.wpw.pim.service.catalog;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.service.product.ProductService;
import com.wpw.pim.web.dto.catalog.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link CatalogService}.
 * Покрывают построение дерева каталога, CRUD секций/категорий/групп,
 * каскадное удаление и подсчёт дочерних элементов.
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock private SectionRepository sectionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductGroupRepository productGroupRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductService productService;

    @InjectMocks
    private CatalogService catalogService;

    @Nested
    @DisplayName("getSectionTree")
    class GetSectionTree {

        @Test
        @DisplayName("строит дерево секций с категориями и группами")
        void getSectionTree_returnsFullTree() {
            Section section = createSection("tools", Map.of("en", "Tools", "ru", "Инструменты"));
            Category category = createCategory(section, "router-bits", Map.of("en", "Router Bits"));
            ProductGroup group = createGroup(category, "straight", "GRP-001", Map.of("en", "Straight Bits"));

            when(sectionRepository.findAllByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(section));
            when(categoryRepository.findAllActiveWithSection()).thenReturn(List.of(category));
            when(productGroupRepository.findAllActiveWithCategory()).thenReturn(List.of(group));

            List<SectionDto> tree = catalogService.getSectionTree("en");

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).name()).isEqualTo("Tools");
            assertThat(tree.get(0).categories()).hasSize(1);
            assertThat(tree.get(0).categories().get(0).name()).isEqualTo("Router Bits");
            assertThat(tree.get(0).categories().get(0).groups()).hasSize(1);
            assertThat(tree.get(0).categories().get(0).groups().get(0).name()).isEqualTo("Straight Bits");
        }

        @Test
        @DisplayName("fallback на en если locale не найден в translations")
        void getSectionTree_fallbackLocale_usesEnglish() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            when(sectionRepository.findAllByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(section));
            when(categoryRepository.findAllActiveWithSection()).thenReturn(List.of());
            when(productGroupRepository.findAllActiveWithCategory()).thenReturn(List.of());

            List<SectionDto> tree = catalogService.getSectionTree("fr");

            assertThat(tree.get(0).name()).isEqualTo("Tools");
        }

        @Test
        @DisplayName("возвращает пустой список если нет активных секций")
        void getSectionTree_noSections_returnsEmpty() {
            when(sectionRepository.findAllByIsActiveTrueOrderBySortOrder()).thenReturn(List.of());
            when(categoryRepository.findAllActiveWithSection()).thenReturn(List.of());
            when(productGroupRepository.findAllActiveWithCategory()).thenReturn(List.of());

            assertThat(catalogService.getSectionTree("en")).isEmpty();
        }
    }

    @Nested
    @DisplayName("createSection")
    class CreateSection {

        @Test
        @DisplayName("создаёт секцию и возвращает DTO")
        void createSection_validRequest_returnsSectionDto() {
            CreateSectionRequest req = new CreateSectionRequest("tools", Map.of("en", "Tools"), 1, true);
            when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
                Section s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });

            SectionDto dto = catalogService.createSection(req, "en");

            assertThat(dto.slug()).isEqualTo("tools");
            assertThat(dto.name()).isEqualTo("Tools");
        }
    }

    @Nested
    @DisplayName("updateSection")
    class UpdateSection {

        @Test
        @DisplayName("обновляет секцию частично")
        void updateSection_partialUpdate_updatesOnlyProvidedFields() {
            Section existing = createSection("tools", Map.of("en", "Tools"));
            UpdateSectionRequest req = new UpdateSectionRequest("new-slug", null, null, null);

            when(sectionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));

            SectionDto dto = catalogService.updateSection(existing.getId(), req, "en");

            assertThat(dto.slug()).isEqualTo("new-slug");
            assertThat(dto.name()).isEqualTo("Tools");
        }

        @Test
        @DisplayName("бросает 404 если секция не найдена")
        void updateSection_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(sectionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.updateSection(id, new UpdateSectionRequest(null, null, null, null), "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("deleteSection")
    class DeleteSection {

        @Test
        @DisplayName("удаляет пустую секцию без cascade")
        void deleteSection_noCascade_noChildren_deletes() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
            when(categoryRepository.findBySectionId(section.getId())).thenReturn(List.of());

            catalogService.deleteSection(section.getId(), false);

            verify(sectionRepository).delete(section);
        }

        @Test
        @DisplayName("бросает CONFLICT при удалении секции с категориями без cascade")
        void deleteSection_noCascade_hasChildren_throwsConflict() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));

            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
            when(categoryRepository.findBySectionId(section.getId())).thenReturn(List.of(category));

            assertThatThrownBy(() -> catalogService.deleteSection(section.getId(), false))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("каскадно удаляет секцию с категориями и группами")
        void deleteSection_cascade_deletesAll() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "straight", "GRP-001", Map.of("en", "Straight"));

            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
            when(categoryRepository.findBySectionId(section.getId())).thenReturn(List.of(category));
            when(productGroupRepository.findByCategoryIdIn(List.of(category.getId()))).thenReturn(List.of(group));

            catalogService.deleteSection(section.getId(), true);

            verify(productService).deleteProductsByGroupIds(List.of(group.getId()));
            verify(productGroupRepository).deleteByCategoryId(category.getId());
            verify(categoryRepository).deleteBySectionId(section.getId());
            verify(sectionRepository).delete(section);
        }
    }

    // --- Categories CRUD ---

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("создаёт категорию и возвращает DTO")
        void createCategory_valid_returnsCategoryDto() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            CreateCategoryRequest req = new CreateCategoryRequest(section.getId(), "bits",
                    Map.of("en", "Router Bits"), 0, true);

            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            CategoryDto dto = catalogService.createCategory(req, "en");

            assertThat(dto.slug()).isEqualTo("bits");
            assertThat(dto.name()).isEqualTo("Router Bits");
        }

        @Test
        @DisplayName("бросает 404 если секция не найдена")
        void createCategory_sectionNotFound_throws404() {
            UUID sectionId = UUID.randomUUID();
            CreateCategoryRequest req = new CreateCategoryRequest(sectionId, "bits",
                    Map.of("en", "Bits"), 0, true);
            when(sectionRepository.findById(sectionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.createCategory(req, "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("обновляет категорию частично")
        void updateCategory_partialUpdate_updatesOnlyProvided() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category existing = createCategory(section, "bits", Map.of("en", "Old Bits"));

            UpdateCategoryRequest req = new UpdateCategoryRequest("new-bits", null, null, null);
            when(categoryRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            CategoryDto dto = catalogService.updateCategory(existing.getId(), req, "en");

            assertThat(dto.slug()).isEqualTo("new-bits");
            assertThat(dto.name()).isEqualTo("Old Bits");
        }

        @Test
        @DisplayName("бросает 404 если категория не найдена")
        void updateCategory_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.updateCategory(id, new UpdateCategoryRequest(null, null, null, null), "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("обновляет все поля категории")
        void updateCategory_fullUpdate() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category existing = createCategory(section, "bits", Map.of("en", "Old"));

            UpdateCategoryRequest req = new UpdateCategoryRequest("new-slug",
                    Map.of("en", "New Name"), 5, false);
            when(categoryRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

            CategoryDto dto = catalogService.updateCategory(existing.getId(), req, "en");

            assertThat(dto.slug()).isEqualTo("new-slug");
            assertThat(dto.name()).isEqualTo("New Name");
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("удаляет пустую категорию без cascade")
        void deleteCategory_noCascade_noGroups_deletes() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(productGroupRepository.findByCategoryId(category.getId())).thenReturn(List.of());

            catalogService.deleteCategory(category.getId(), false);

            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("бросает CONFLICT при удалении категории с группами без cascade")
        void deleteCategory_noCascade_hasGroups_throwsConflict() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "straight", "G1", Map.of("en", "Straight"));

            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(productGroupRepository.findByCategoryId(category.getId())).thenReturn(List.of(group));

            assertThatThrownBy(() -> catalogService.deleteCategory(category.getId(), false))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("каскадно удаляет категорию с группами")
        void deleteCategory_cascade_deletesAll() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "straight", "G1", Map.of("en", "Straight"));

            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(productGroupRepository.findByCategoryId(category.getId())).thenReturn(List.of(group));

            catalogService.deleteCategory(category.getId(), true);

            verify(productService).deleteProductsByGroupIds(List.of(group.getId()));
            verify(productGroupRepository).deleteByCategoryId(category.getId());
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("бросает 404 если категория не найдена")
        void deleteCategory_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.deleteCategory(id, false))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // --- Product Groups CRUD ---

    @Nested
    @DisplayName("createProductGroup")
    class CreateProductGroupTests {

        @Test
        @DisplayName("создаёт группу продуктов и возвращает DTO")
        void createProductGroup_valid_returnsDto() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));

            CreateProductGroupRequest req = new CreateProductGroupRequest(category.getId(),
                    "straight", "GRP-001", Map.of("en", "Straight Bits"), 0, true);

            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(productGroupRepository.save(any(ProductGroup.class))).thenAnswer(inv -> {
                ProductGroup g = inv.getArgument(0);
                g.setId(UUID.randomUUID());
                return g;
            });

            ProductGroupDto dto = catalogService.createProductGroup(req, "en");

            assertThat(dto.slug()).isEqualTo("straight");
            assertThat(dto.groupCode()).isEqualTo("GRP-001");
            assertThat(dto.name()).isEqualTo("Straight Bits");
        }

        @Test
        @DisplayName("бросает 404 если категория не найдена")
        void createProductGroup_categoryNotFound_throws404() {
            UUID catId = UUID.randomUUID();
            CreateProductGroupRequest req = new CreateProductGroupRequest(catId,
                    "slug", "code", Map.of("en", "Name"), 0, true);
            when(categoryRepository.findById(catId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.createProductGroup(req, "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("updateProductGroup")
    class UpdateProductGroupTests {

        @Test
        @DisplayName("обновляет группу продуктов частично")
        void updateProductGroup_partialUpdate() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "straight", "GRP-001", Map.of("en", "Straight"));

            UpdateProductGroupRequest req = new UpdateProductGroupRequest("new-slug", null, null, null, null);

            when(productGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
            when(productGroupRepository.save(any(ProductGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductGroupDto dto = catalogService.updateProductGroup(group.getId(), req, "en");

            assertThat(dto.slug()).isEqualTo("new-slug");
            assertThat(dto.name()).isEqualTo("Straight");
        }

        @Test
        @DisplayName("бросает 404 если группа не найдена")
        void updateProductGroup_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(productGroupRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.updateProductGroup(id,
                    new UpdateProductGroupRequest(null, null, null, null, null), "en"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("обновляет все поля группы")
        void updateProductGroup_fullUpdate() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "old-slug", "OLD-CODE", Map.of("en", "Old"));

            UpdateProductGroupRequest req = new UpdateProductGroupRequest(
                    "new-slug", "NEW-CODE", Map.of("en", "New Name"), 10, false);

            when(productGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
            when(productGroupRepository.save(any(ProductGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductGroupDto dto = catalogService.updateProductGroup(group.getId(), req, "en");

            assertThat(dto.slug()).isEqualTo("new-slug");
            assertThat(dto.groupCode()).isEqualTo("NEW-CODE");
            assertThat(dto.name()).isEqualTo("New Name");
        }
    }

    @Nested
    @DisplayName("deleteProductGroup")
    class DeleteProductGroupTests {

        @Test
        @DisplayName("удаляет группу и её продукты")
        void deleteProductGroup_existing_deletes() {
            Section section = createSection("tools", Map.of("en", "Tools"));
            Category category = createCategory(section, "bits", Map.of("en", "Bits"));
            ProductGroup group = createGroup(category, "straight", "G1", Map.of("en", "Straight"));

            when(productGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));

            catalogService.deleteProductGroup(group.getId());

            verify(productService).deleteProductsByGroupIds(List.of(group.getId()));
            verify(productGroupRepository).delete(group);
        }

        @Test
        @DisplayName("бросает 404 если группа не найдена")
        void deleteProductGroup_notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(productGroupRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogService.deleteProductGroup(id))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // --- Reorder ---

    @Nested
    @DisplayName("reorderSections")
    class ReorderSectionsTests {

        @Test
        @DisplayName("обновляет sortOrder для секций")
        void reorderSections_updatesSortOrder() {
            Section s1 = createSection("s1", Map.of("en", "S1"));
            Section s2 = createSection("s2", Map.of("en", "S2"));

            when(sectionRepository.findById(s1.getId())).thenReturn(Optional.of(s1));
            when(sectionRepository.findById(s2.getId())).thenReturn(Optional.of(s2));
            when(sectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReorderRequest req = new ReorderRequest(List.of(
                    new ReorderRequest.ReorderItem(s1.getId(), 2),
                    new ReorderRequest.ReorderItem(s2.getId(), 1)));

            catalogService.reorderSections(req);

            verify(sectionRepository, times(2)).save(any(Section.class));
        }
    }

    @Nested
    @DisplayName("reorderCategories")
    class ReorderCategoriesTests {

        @Test
        @DisplayName("обновляет sortOrder для категорий")
        void reorderCategories_updatesSortOrder() {
            Section section = createSection("s", Map.of("en", "S"));
            Category c1 = createCategory(section, "c1", Map.of("en", "C1"));

            when(categoryRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReorderRequest req = new ReorderRequest(List.of(
                    new ReorderRequest.ReorderItem(c1.getId(), 5)));

            catalogService.reorderCategories(req);

            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("reorderProductGroups")
    class ReorderProductGroupsTests {

        @Test
        @DisplayName("обновляет sortOrder для групп продуктов")
        void reorderProductGroups_updatesSortOrder() {
            Section section = createSection("s", Map.of("en", "S"));
            Category category = createCategory(section, "c", Map.of("en", "C"));
            ProductGroup g1 = createGroup(category, "g1", "G1", Map.of("en", "G1"));

            when(productGroupRepository.findById(g1.getId())).thenReturn(Optional.of(g1));
            when(productGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReorderRequest req = new ReorderRequest(List.of(
                    new ReorderRequest.ReorderItem(g1.getId(), 3)));

            catalogService.reorderProductGroups(req);

            verify(productGroupRepository).save(any(ProductGroup.class));
        }
    }

    // --- Children count ---

    @Nested
    @DisplayName("getChildrenCount")
    class ChildrenCount {

        @Test
        @DisplayName("возвращает количество дочерних категорий и групп")
        void getChildrenCount_returnsCorrectCounts() {
            UUID sectionId = UUID.randomUUID();
            Category cat = new Category();
            cat.setId(UUID.randomUUID());

            when(categoryRepository.countBySectionId(sectionId)).thenReturn(2L);
            when(categoryRepository.findBySectionId(sectionId)).thenReturn(List.of(cat));
            when(productGroupRepository.countByCategoryIdIn(List.of(cat.getId()))).thenReturn(5L);

            ChildrenCountResponse response = catalogService.getChildrenCount(sectionId);

            assertThat(response.categories()).isEqualTo(2L);
            assertThat(response.productGroups()).isEqualTo(5L);
        }

        @Test
        @DisplayName("возвращает 0 групп если нет категорий")
        void getChildrenCount_noCategories_zeroGroups() {
            UUID sectionId = UUID.randomUUID();
            when(categoryRepository.countBySectionId(sectionId)).thenReturn(0L);
            when(categoryRepository.findBySectionId(sectionId)).thenReturn(List.of());

            ChildrenCountResponse response = catalogService.getChildrenCount(sectionId);

            assertThat(response.categories()).isZero();
            assertThat(response.productGroups()).isZero();
        }
    }

    @Nested
    @DisplayName("getCategoryChildrenCount")
    class CategoryChildrenCount {

        @Test
        @DisplayName("возвращает количество групп в категории")
        void getCategoryChildrenCount_returnsCount() {
            UUID categoryId = UUID.randomUUID();
            when(productGroupRepository.countByCategoryId(categoryId)).thenReturn(3L);

            long count = catalogService.getCategoryChildrenCount(categoryId);

            assertThat(count).isEqualTo(3L);
        }
    }

    // --- Helpers ---

    private Section createSection(String slug, Map<String, String> translations) {
        Section s = new Section();
        s.setId(UUID.randomUUID());
        s.setSlug(slug);
        s.setTranslations(new HashMap<>(translations));
        s.setSortOrder(0);
        s.setActive(true);
        return s;
    }

    private Category createCategory(Section section, String slug, Map<String, String> translations) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setSection(section);
        c.setSlug(slug);
        c.setTranslations(new HashMap<>(translations));
        c.setSortOrder(0);
        c.setActive(true);
        return c;
    }

    private ProductGroup createGroup(Category category, String slug, String groupCode, Map<String, String> translations) {
        ProductGroup g = new ProductGroup();
        g.setId(UUID.randomUUID());
        g.setCategory(category);
        g.setSlug(slug);
        g.setGroupCode(groupCode);
        g.setTranslations(new HashMap<>(translations));
        g.setSortOrder(0);
        g.setActive(true);
        return g;
    }
}
