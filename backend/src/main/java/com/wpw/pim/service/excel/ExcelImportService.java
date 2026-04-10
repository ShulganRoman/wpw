package com.wpw.pim.service.excel;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.catalog.Section;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
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
import com.wpw.pim.service.excel.classifier.MaterialClassifier;
import com.wpw.pim.service.excel.classifier.MachineClassifier;
import com.wpw.pim.service.excel.config.ExcelImportProperties;
import com.wpw.pim.service.excel.dto.*;
import com.wpw.pim.service.excel.parser.GroupsSheetParser;
import com.wpw.pim.service.excel.parser.ProductsSheetParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import com.wpw.pim.service.excel.validation.ImportValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import com.wpw.pim.domain.enums.BoreType;
import com.wpw.pim.domain.enums.RotationDirection;
import com.wpw.pim.domain.enums.StockStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис массового импорта из Excel.
 *
 * Поток выполнения:
 *  1. validate()  — разбор + валидация, без записи в БД → возвращает ValidationReport
 *  2. execute()   — полный импорт → возвращает Markdown-отчёт
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ExcelImportProperties      props;
    private final ProductsSheetParser        productsParser;
    private final GroupsSheetParser          groupsParser;
    private final ImportValidator            validator;
    private final MaterialClassifier         materialClassifier;
    private final MachineClassifier          machineClassifier;
    private final CuttingTypeNormalizer      cuttingTypeNormalizer;
    private final ImportReportGenerator      reportGenerator;

    private final SectionRepository         sectionRepo;
    private final CategoryRepository        categoryRepo;
    private final ProductGroupRepository    groupRepo;
    private final ProductRepository         productRepo;
    private final ProductTranslationRepository translationRepo;

    // -------------------------------------------------------------------------
    // Validate
    // -------------------------------------------------------------------------

    public ValidationReport validate(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(is)) {

            var productsSheet = wb.getSheet(props.getProductsSheet().getName());
            var groupsSheet   = wb.getSheet(props.getGroupsSheet().getName());

            if (productsSheet == null) {
                throw new IllegalArgumentException(
                    "Лист «" + props.getProductsSheet().getName() + "» не найден в файле");
            }
            if (groupsSheet == null) {
                throw new IllegalArgumentException(
                    "Лист «" + props.getGroupsSheet().getName() + "» не найден в файле");
            }

            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            List<RawProductRow> products = productsParser.parse(productsSheet, evaluator);
            List<RawGroupRow>   groups   = groupsParser.parse(groupsSheet, evaluator);
            List<String>        unknown  = productsParser.unknownHeaders(productsSheet);

            return validator.validate(products, groups, unknown);
        }
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Transactional
    public String execute(MultipartFile file) throws Exception {
        Instant start = Instant.now();

        try (InputStream is = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(is)) {

            var productsSheet = wb.getSheet(props.getProductsSheet().getName());
            var groupsSheet   = wb.getSheet(props.getGroupsSheet().getName());

            if (productsSheet == null || groupsSheet == null) {
                throw new IllegalArgumentException("Файл не содержит нужных листов");
            }

            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            List<RawProductRow> products = productsParser.parse(productsSheet, evaluator);
            List<RawGroupRow>   groups   = groupsParser.parse(groupsSheet, evaluator);

            StatsAccumulator acc = new StatsAccumulator();
            acc.totalProductRows = products.size();

            // 1. Upsert каталога (section → category → group)
            Section defaultSection = findOrCreateSection(acc);
            Map<String, Category>     categoryCache = new HashMap<>();
            Map<String, ProductGroup> groupCache    = new HashMap<>();

            for (RawGroupRow g : groups) {
                if (g.getGroupId() == null || g.getCategoryName() == null) continue;
                try {
                    Category cat = findOrCreateCategory(g.getCategoryName(), defaultSection,
                        categoryCache, acc);
                    findOrCreateGroup(g, cat, groupCache, acc);
                } catch (Exception e) {
                    acc.errors.add("Группа " + g.getGroupId() + " (строка " + g.getRowNum() + "): " + e.getMessage());
                }
            }

            // 2. Upsert товаров
            for (RawProductRow p : products) {
                if (p.getToolNo() == null || p.getToolNo().isBlank()) {
                    acc.skipped++;
                    continue;
                }
                try {
                    importProduct(p, groupCache, acc);
                } catch (Exception e) {
                    acc.errors.add("Товар " + p.getToolNo() + " (строка " + p.getRowNum() + "): " + e.getMessage());
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
        String categoryName,
        Section section,
        Map<String, Category> cache,
        StatsAccumulator acc
    ) {
        String slug = slugify(categoryName);
        if (cache.containsKey(slug)) return cache.get(slug);

        Category cat = categoryRepo.findBySlug(slug).orElseGet(() -> {
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
        RawGroupRow g,
        Category category,
        Map<String, ProductGroup> cache,
        StatsAccumulator acc
    ) {
        String baseSlug = slugify(g.getGroupId());
        // Ключ кеша уникален в рамках категории
        String cacheKey = category.getId() + ":" + baseSlug;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        // Сначала ищем по (категория + slug) — правильный поиск
        Optional<ProductGroup> existing = groupRepo.findByCategoryIdAndSlug(category.getId(), baseSlug);
        if (existing.isPresent()) {
            acc.groupsFound++;
            cache.put(cacheKey, existing.get());
            return existing.get();
        }

        // Slug свободен глобально — используем простой;
        // занят другой категорией — добавляем префикс категории
        String slug = groupRepo.findBySlug(baseSlug).isEmpty()
            ? baseSlug
            : slugify(category.getTranslations().getOrDefault("en", category.getSlug())) + "-" + baseSlug;

        ProductGroup pg = new ProductGroup();
        pg.setSlug(slug);
        pg.setGroupCode(g.getGroupCode() != null ? g.getGroupCode() : g.getGroupId());
        pg.setTranslations(buildTranslations(g.getGroupName()));
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

    private void importProduct(
        RawProductRow p,
        Map<String, ProductGroup> groupCache,
        StatsAccumulator acc
    ) {
        boolean isNew = !productRepo.existsByToolNo(p.getToolNo());

        Product product = productRepo.findByToolNo(p.getToolNo())
            .orElseGet(Product::new);

        product.setToolNo(p.getToolNo());
        product.setAltToolNo(p.getAltToolNo());

        if (p.getProductType() != null) {
            try { product.setProductType(ProductType.valueOf(p.getProductType())); }
            catch (IllegalArgumentException ignored) { }
        } else {
            product.setProductType(ProductType.main);
        }

        if (p.getStatus() != null) {
            try { product.setStatus(ProductStatus.valueOf(p.getStatus())); }
            catch (IllegalArgumentException ignored) { }
        } else {
            product.setStatus(ProductStatus.active);
        }

        if (p.getOrderable() != null) {
            product.setOrderable(truthy(p.getOrderable()));
        }

        if (p.getCatalogPage() != null) {
            try { product.setCatalogPage(Short.parseShort(p.getCatalogPage())); }
            catch (NumberFormatException ignored) { }
        }

        if (p.getGroupId() != null) {
            ProductGroup group = groupCache.get(slugify(p.getGroupId()));
            if (group != null) product.setGroup(group);
        }

        // Application Tags
        if (p.getApplicationTags() != null && !p.getApplicationTags().isBlank()) {
            Set<String> tags = Arrays.stream(p.getApplicationTags().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            product.setOperationCodes(tags);
        }

        // Materials: prefer explicit Tool/Workpiece columns, fallback to classifier
        if (notBlank(p.getToolMaterials()) || notBlank(p.getWorkpieceMaterials())) {
            product.setToolMaterials(splitToSet(p.getToolMaterials()));
            product.setWorkpieceMaterials(splitToSet(p.getWorkpieceMaterials()));
        } else {
            var matClass = materialClassifier.classify(p.getMaterials());
            product.setToolMaterials(matClass.toolMaterials());
            product.setWorkpieceMaterials(matClass.workpieceMaterials());
        }

        // Machines: prefer explicit columns, fallback to classifier
        if (notBlank(p.getMachineTypes())) {
            product.setMachineTypes(splitToSet(p.getMachineTypes()));
            if (notBlank(p.getMachineBrands())) {
                product.setMachineBrands(splitToSet(p.getMachineBrands()));
            }
        } else {
            var machClass = machineClassifier.classify(p.getMachines());
            product.setMachineTypes(machClass.machineTypes());
            product.setMachineBrands(machClass.machineBrands());
        }

        if (notBlank(p.getMachineBrands()) && !notBlank(p.getMachineTypes())) {
            product.setMachineBrands(splitToSet(p.getMachineBrands()));
        }

        // Атрибуты — создаём/обновляем ДО save, чтобы cascade CascadeType.ALL их сохранил
        ProductAttributes attr = product.getAttributes();
        if (attr == null) {
            attr = new ProductAttributes();
            attr.setProduct(product);
            product.setAttributes(attr);
        }
        fillAttributes(attr, p);

        // Сохраняем продукт — cascade сохраняет/обновляет attributes
        product = productRepo.save(product);

        // Перевод EN (отдельная таблица, нет каскада)
        upsertTranslation(product, p);

        if (isNew) acc.created++;
        else acc.updated++;
    }

    private void fillAttributes(ProductAttributes attr, RawProductRow p) {
        attr.setDMm(decimal(p.getDMm()));
        attr.setD1Mm(decimal(p.getD1Mm()));
        attr.setD2Mm(decimal(p.getD2Mm()));
        attr.setBMm(decimal(p.getBMm()));
        attr.setB1Mm(decimal(p.getB1Mm()));
        attr.setLMm(decimal(p.getLMm()));
        attr.setL1Mm(decimal(p.getL1Mm()));
        attr.setRMm(decimal(p.getRMm()));
        attr.setAMm(decimal(p.getAMm()));
        attr.setAngleDeg(decimal(p.getAngleDeg()));
        attr.setShankMm(decimal(p.getShankMm()));
        attr.setShankInch(p.getShankInch());
        attr.setFlutes(shortVal(p.getFlutes()));
        attr.setBladeNo(shortVal(p.getBladeNo()));
        if (p.getCuttingType() != null) {
            attr.setCuttingType(cuttingTypeNormalizer.normalize(p.getCuttingType()));
        }

        // Rotation Direction
        if (p.getRotationDirection() != null) {
            try { attr.setRotationDirection(RotationDirection.valueOf(p.getRotationDirection().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }

        // Bore Type
        if (p.getBoreType() != null) {
            try { attr.setBoreType(BoreType.valueOf(p.getBoreType().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }

        // Ball Bearing: explicit flag takes priority
        if (p.getHasBallBearing() != null) {
            attr.setHasBallBearing(truthy(p.getHasBallBearing()));
            if (p.getBallBearing() != null && !p.getBallBearing().isBlank()) {
                attr.setBallBearingCode(p.getBallBearing());
            }
        } else if (p.getBallBearing() != null && !p.getBallBearing().isBlank()) {
            attr.setHasBallBearing(true);
            attr.setBallBearingCode(p.getBallBearing());
        }

        // Retainer: explicit flag takes priority
        if (p.getHasRetainer() != null) {
            attr.setHasRetainer(truthy(p.getHasRetainer()));
            if (p.getRetainer() != null && !p.getRetainer().isBlank()) {
                attr.setRetainerCode(p.getRetainer());
            }
        } else if (p.getRetainer() != null && !p.getRetainer().isBlank()) {
            attr.setHasRetainer(true);
            attr.setRetainerCode(p.getRetainer());
        }

        attr.setCanResharpen(truthy(p.getCanResharpen()));

        attr.setEan13(p.getEan13());
        attr.setUpc12(p.getUpc12());
        attr.setHsCode(p.getHsCode());
        attr.setCountryOfOrigin(p.getCountryOfOrigin());
        attr.setWeightG(intVal(p.getWeightG()));
        attr.setPkgQty(shortVal(p.getPkgQty()));
        attr.setCartonQty(shortVal(p.getCartonQty()));
        attr.setStockQty(intVal(p.getStockQty()));

        // Stock Status
        if (p.getStockStatus() != null) {
            try { attr.setStockStatus(StockStatus.valueOf(p.getStockStatus().toLowerCase().trim())); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private void upsertTranslation(Product product, RawProductRow p) {
        boolean hasTranslationData = p.getName() != null || p.getDescription() != null
            || p.getGroupName() != null || p.getShortDescription() != null
            || p.getLongDescription() != null;
        if (!hasTranslationData) return;

        ProductTranslationId id = new ProductTranslationId(product.getId(), "en");
        ProductTranslation translation = translationRepo.findById(id)
            .orElseGet(() -> {
                ProductTranslation t = new ProductTranslation();
                t.setId(id);
                t.setProduct(product);
                return t;
            });

        // Name: explicit column takes priority, then Description, then fallback
        if (p.getName() != null) {
            translation.setName(p.getName());
        } else {
            String name = p.getDescription() != null ? p.getDescription()
                : (p.getGroupName() != null ? p.getToolNo() + " " + p.getGroupName() : p.getToolNo());
            translation.setName(name);
        }

        if (p.getShortDescription() != null) {
            translation.setShortDescription(p.getShortDescription());
        }

        if (p.getLongDescription() != null) {
            translation.setLongDescription(p.getLongDescription());
        }

        if (p.getApplications() != null && !p.getApplications().isBlank()) {
            translation.setApplications(p.getApplications());
        }

        translationRepo.save(translation);
    }

    // -------------------------------------------------------------------------
    // Утилиты
    // -------------------------------------------------------------------------

    private static String slugify(String s) {
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

    private static boolean truthy(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase().trim();
        return lower.equals("yes") || lower.equals("true") || lower.equals("1") || lower.equals("y");
    }

    private static Integer intVal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
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
    // Внутренний аккумулятор статистики
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
