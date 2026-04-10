package com.wpw.pim.service.excel;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.domain.enums.*;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.config.ExcelImportV4Properties;
import com.wpw.pim.service.excel.dto.*;
import com.wpw.pim.service.excel.parser.V4SheetParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import com.wpw.pim.service.excel.validation.ImportV4Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис импорта формата v4 — один лист "Products", без отдельного листа Groups.
 * <p>
 * Группы создаются автоматически из пар (Category, Group Name).
 * Это упрощает работу пользователя: не нужно заполнять Group ID,
 * достаточно указать Category и Group Name в каждой строке продукта.
 * </p>
 *
 * @see ExcelImportService аналогичный сервис для стандартного формата с двумя листами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportV4Service {

    private final ExcelImportV4Properties props;
    private final V4SheetParser           parser;
    private final ImportV4Validator        validator;
    private final CuttingTypeNormalizer    cuttingTypeNormalizer;
    private final ImportReportGenerator    reportGenerator;

    private final SectionRepository            sectionRepo;
    private final CategoryRepository           categoryRepo;
    private final ProductGroupRepository       groupRepo;
    private final ProductRepository            productRepo;
    private final ProductTranslationRepository translationRepo;

    // -------------------------------------------------------------------------
    // Validate — без записи в БД
    // -------------------------------------------------------------------------

    /**
     * Валидация файла формата v4: парсит лист, проверяет данные.
     *
     * @param file загруженный Excel-файл
     * @return отчёт о валидации
     * @throws IllegalArgumentException если лист "Products" не найден
     */
    public ValidationReport validate(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {

            var sheet = wb.getSheet(props.getSheetName());
            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Лист \u00ab" + props.getSheetName() + "\u00bb не найден");
            }

            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            List<RawV4Row> rows = parser.parse(sheet, evaluator);
            List<String> unknown = parser.unknownHeaders(sheet);

            return validator.validate(rows, unknown);
        }
    }

    // -------------------------------------------------------------------------
    // Execute — полный импорт
    // -------------------------------------------------------------------------

    /**
     * Выполняет импорт из файла формата v4.
     * <p>
     * Порядок действий:
     * <ol>
     *   <li>Парсит лист Products</li>
     *   <li>Создаёт/находит каталожную структуру (Section → Category → ProductGroup)</li>
     *   <li>Upsert каждого продукта</li>
     * </ol>
     *
     * @param file загруженный Excel-файл
     * @return Markdown-отчёт со статистикой импорта
     */
    @Transactional
    public String execute(MultipartFile file) throws Exception {
        Instant start = Instant.now();

        try (InputStream is = file.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {

            var sheet = wb.getSheet(props.getSheetName());
            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Лист \u00ab" + props.getSheetName() + "\u00bb не найден");
            }

            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            List<RawV4Row> rows = parser.parse(sheet, evaluator);

            StatsAccumulator acc = new StatsAccumulator();
            acc.totalProductRows = rows.size();

            // 1. Upsert каталога
            Section defaultSection = findOrCreateSection(acc);
            Map<String, Category>     categoryCache = new HashMap<>();
            Map<String, ProductGroup> groupCache    = new HashMap<>();

            for (RawV4Row row : rows) {
                if (notBlank(row.getCategoryName()) && notBlank(row.getGroupName())) {
                    try {
                        Category cat = findOrCreateCategory(
                            row.getCategoryName(), defaultSection, categoryCache, acc);
                        findOrCreateGroup(
                            row.getGroupName(), cat, groupCache, acc);
                    } catch (Exception e) {
                        acc.errors.add("Каталог (строка " + row.getRowNum() + "): " + e.getMessage());
                    }
                }
            }

            // 2. Upsert товаров
            for (RawV4Row row : rows) {
                if (row.getToolNo() == null || row.getToolNo().isBlank()) {
                    acc.skipped++;
                    continue;
                }
                try {
                    importProduct(row, categoryCache, groupCache, acc);
                } catch (Exception e) {
                    acc.errors.add("Товар " + row.getToolNo() + " (строка " + row.getRowNum() + "): "
                        + e.getMessage());
                    acc.skipped++;
                }
            }

            Duration duration = Duration.between(start, Instant.now());
            ImportStats stats = acc.build(duration);
            return reportGenerator.generate(stats);
        }
    }

    // -------------------------------------------------------------------------
    // Каталог
    // -------------------------------------------------------------------------

    private Section findOrCreateSection(StatsAccumulator acc) {
        String slug = "wpw-tools";
        return sectionRepo.findBySlug(slug).orElseGet(() -> {
            Section s = new Section();
            s.setSlug(slug);
            s.setTranslations(Map.of("en", "WPW Professional Cutting Tools"));
            s.setSortOrder(0);
            acc.sectionsCreated++;
            return sectionRepo.save(s);
        });
    }

    private Category findOrCreateCategory(
        String categoryName, Section section,
        Map<String, Category> cache, StatsAccumulator acc
    ) {
        String slug = slugify(categoryName);
        if (cache.containsKey(slug)) return cache.get(slug);

        Category cat = categoryRepo.findBySlug(slug)
            .or(() -> categoryRepo.findByNameEnIgnoreCase(categoryName))
            .orElseGet(() -> {
                Category c = new Category();
                c.setSlug(slug);
                c.setSection(section);
                c.setTranslations(Map.of("en", categoryName));
                c.setSortOrder(cache.size());
                acc.categoriesCreated++;
                return categoryRepo.save(c);
            });

        if (!cache.containsKey(slug)) acc.categoriesFound++;
        cache.put(slug, cat);
        return cat;
    }

    private ProductGroup findOrCreateGroup(
        String groupName, Category category,
        Map<String, ProductGroup> cache, StatsAccumulator acc
    ) {
        String baseSlug = slugify(groupName);
        // Ключ кеша — уникален в рамках категории
        String cacheKey = category.getId() + ":" + baseSlug;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        // Ищем по (categoryId, slug), fallback — по (categoryId, name)
        Optional<ProductGroup> existing = groupRepo.findByCategoryIdAndSlug(category.getId(), baseSlug)
            .or(() -> groupRepo.findByCategoryIdAndNameEnIgnoreCase(category.getId(), groupName));
        if (existing.isPresent()) {
            acc.groupsFound++;
            cache.put(cacheKey, existing.get());
            return existing.get();
        }

        // Slug свободен глобально — создаём с простым slug
        // Slug занят другой категорией — добавляем префикс категории для уникальности
        String slug = groupRepo.findBySlug(baseSlug).isEmpty()
            ? baseSlug
            : slugify(category.getTranslations().getOrDefault("en", category.getSlug())) + "-" + baseSlug;

        ProductGroup pg = new ProductGroup();
        pg.setSlug(slug);
        pg.setGroupCode(baseSlug);
        pg.setTranslations(buildTranslations(groupName));
        pg.setCategory(category);
        pg.setSortOrder(cache.size());
        acc.groupsCreated++;
        ProductGroup saved = groupRepo.save(pg);
        cache.put(cacheKey, saved);
        return saved;
    }

    // -------------------------------------------------------------------------
    // Товары
    // -------------------------------------------------------------------------

    private void importProduct(RawV4Row row,
                               Map<String, Category> categoryCache,
                               Map<String, ProductGroup> groupCache,
                               StatsAccumulator acc) {
        boolean isNew = !productRepo.existsByToolNo(row.getToolNo());

        Product product = productRepo.findByToolNo(row.getToolNo())
            .orElseGet(Product::new);

        product.setToolNo(row.getToolNo());
        product.setAltToolNo(row.getAltToolNo());

        // Status
        if (row.getStatus() != null) {
            try { product.setStatus(ProductStatus.valueOf(row.getStatus().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        } else {
            product.setStatus(ProductStatus.active);
        }

        // Orderable
        if (row.getOrderable() != null) {
            product.setOrderable(truthy(row.getOrderable()));
        } else {
            product.setOrderable(true);
        }

        // Product Type
        if (row.getProductType() != null) {
            try { product.setProductType(ProductType.valueOf(row.getProductType().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        } else {
            product.setProductType(ProductType.main);
        }

        // Catalog Page
        if (row.getCatalogPage() != null) {
            try { product.setCatalogPage(Short.parseShort(row.getCatalogPage())); }
            catch (NumberFormatException ignored) { }
        }

        // Group — ключ кэша: categoryId:groupSlug (как в findOrCreateGroup)
        if (notBlank(row.getGroupName()) && notBlank(row.getCategoryName())) {
            Category cat = categoryCache.get(slugify(row.getCategoryName()));
            if (cat != null) {
                String cacheKey = cat.getId() + ":" + slugify(row.getGroupName());
                ProductGroup group = groupCache.get(cacheKey);
                if (group != null) product.setGroup(group);
            }
        }

        // Application Tags
        if (notBlank(row.getApplicationTags())) {
            product.setOperationCodes(splitToSet(row.getApplicationTags()));
        }

        // Materials — напрямую из колонок
        if (notBlank(row.getToolMaterials())) {
            product.setToolMaterials(splitToSet(row.getToolMaterials()));
        }
        if (notBlank(row.getWorkpieceMaterials())) {
            product.setWorkpieceMaterials(splitToSet(row.getWorkpieceMaterials()));
        }

        // Machines — напрямую из колонок
        if (notBlank(row.getMachineTypes())) {
            product.setMachineTypes(splitToSet(row.getMachineTypes()));
        }
        if (notBlank(row.getMachineBrands())) {
            product.setMachineBrands(splitToSet(row.getMachineBrands()));
        }

        // Атрибуты
        ProductAttributes attr = product.getAttributes();
        if (attr == null) {
            attr = new ProductAttributes();
            attr.setProduct(product);
            product.setAttributes(attr);
        }
        fillAttributes(attr, row);

        product = productRepo.save(product);

        // Перевод EN
        upsertTranslation(product, row);

        if (isNew) acc.created++;
        else acc.updated++;
    }

    private void fillAttributes(ProductAttributes attr, RawV4Row row) {
        attr.setDMm(decimal(row.getDMm()));
        attr.setD1Mm(decimal(row.getD1Mm()));
        attr.setD2Mm(decimal(row.getD2Mm()));
        attr.setBMm(decimal(row.getBMm()));
        attr.setB1Mm(decimal(row.getB1Mm()));
        attr.setLMm(decimal(row.getLMm()));
        attr.setL1Mm(decimal(row.getL1Mm()));
        attr.setRMm(decimal(row.getRMm()));
        attr.setAMm(decimal(row.getAMm()));
        attr.setAngleDeg(decimal(row.getAngleDeg()));
        attr.setShankMm(decimal(row.getShankMm()));
        attr.setShankInch(row.getShankInch());
        attr.setFlutes(shortVal(row.getFlutes()));
        attr.setBladeNo(shortVal(row.getBladeNo()));

        if (row.getCuttingType() != null) {
            attr.setCuttingType(cuttingTypeNormalizer.normalize(row.getCuttingType()));
        }

        // Rotation Direction
        if (row.getRotationDirection() != null) {
            try { attr.setRotationDirection(RotationDirection.valueOf(row.getRotationDirection().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }

        // Bore Type
        if (row.getBoreType() != null) {
            try { attr.setBoreType(BoreType.valueOf(row.getBoreType().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }

        // Ball Bearing
        if (row.getHasBallBearing() != null) {
            attr.setHasBallBearing(truthy(row.getHasBallBearing()));
            if (row.getBallBearing() != null && !row.getBallBearing().isBlank()) {
                attr.setBallBearingCode(row.getBallBearing());
            }
        } else if (row.getBallBearing() != null && !row.getBallBearing().isBlank()) {
            attr.setHasBallBearing(true);
            attr.setBallBearingCode(row.getBallBearing());
        }

        // Retainer
        if (row.getHasRetainer() != null) {
            attr.setHasRetainer(truthy(row.getHasRetainer()));
            if (row.getRetainer() != null && !row.getRetainer().isBlank()) {
                attr.setRetainerCode(row.getRetainer());
            }
        } else if (row.getRetainer() != null && !row.getRetainer().isBlank()) {
            attr.setHasRetainer(true);
            attr.setRetainerCode(row.getRetainer());
        }

        attr.setCanResharpen(truthy(row.getCanResharpen()));

        attr.setEan13(row.getEan13());
        attr.setUpc12(row.getUpc12());
        attr.setHsCode(row.getHsCode());
        attr.setCountryOfOrigin(row.getCountryOfOrigin());
        attr.setWeightG(intVal(row.getWeightG()));
        attr.setPkgQty(shortVal(row.getPkgQty()));
        attr.setCartonQty(shortVal(row.getCartonQty()));
        attr.setStockQty(intVal(row.getStockQty()));

        // Stock Status
        if (row.getStockStatus() != null) {
            try { attr.setStockStatus(StockStatus.valueOf(row.getStockStatus().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private void upsertTranslation(Product product, RawV4Row row) {
        boolean hasData = row.getName() != null || row.getShortDescription() != null
            || row.getLongDescription() != null;
        if (!hasData) return;

        ProductTranslationId id = new ProductTranslationId(product.getId(), "en");
        ProductTranslation translation = translationRepo.findById(id)
            .orElseGet(() -> {
                ProductTranslation t = new ProductTranslation();
                t.setId(id);
                t.setProduct(product);
                return t;
            });

        if (row.getName() != null) {
            translation.setName(row.getName());
        } else {
            // Fallback: toolNo + groupName
            String name = row.getGroupName() != null
                ? row.getToolNo() + " " + row.getGroupName()
                : row.getToolNo();
            translation.setName(name);
        }

        if (row.getShortDescription() != null) {
            translation.setShortDescription(row.getShortDescription());
        }

        if (row.getLongDescription() != null) {
            translation.setLongDescription(row.getLongDescription());
        }

        if (notBlank(row.getApplicationTags())) {
            translation.setApplications(row.getApplicationTags());
        }

        translationRepo.save(translation);
    }

    // -------------------------------------------------------------------------
    // Утилиты
    // -------------------------------------------------------------------------

    static String slugify(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }

    private static Map<String, String> buildTranslations(String enName) {
        Map<String, String> map = new HashMap<>();
        if (enName != null) map.put("en", enName);
        return map;
    }

    private static BigDecimal decimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private static Short shortVal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Short.parseShort(s); } catch (NumberFormatException e) { return null; }
    }

    private static Integer intVal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private static boolean truthy(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase().trim();
        return lower.equals("yes") || lower.equals("true") || lower.equals("1") || lower.equals("y");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static Set<String> splitToSet(String s) {
        if (s == null || s.isBlank()) return new HashSet<>();
        return Arrays.stream(s.split(","))
            .map(String::trim).filter(v -> !v.isEmpty())
            .collect(Collectors.toCollection(HashSet::new));
    }

    // -------------------------------------------------------------------------
    // Аккумулятор статистики
    // -------------------------------------------------------------------------

    private static class StatsAccumulator {
        int totalProductRows;
        int created, updated, skipped;
        int sectionsCreated, categoriesCreated, categoriesFound, groupsCreated, groupsFound;
        final List<String> errors   = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        ImportStats build(Duration duration) {
            return ImportStats.builder()
                .importedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .duration(duration)
                .totalProductRows(totalProductRows)
                .productsCreated(created)
                .productsUpdated(updated)
                .productsSkipped(skipped)
                .sectionsCreated(sectionsCreated)
                .categoriesCreated(categoriesCreated)
                .categoriesFound(categoriesFound)
                .groupsCreated(groupsCreated)
                .groupsFound(groupsFound)
                .executionErrors(errors)
                .executionWarnings(warnings)
                .build();
        }
    }
}
