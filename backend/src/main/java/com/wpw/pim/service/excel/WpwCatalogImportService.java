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
import com.wpw.pim.domain.operation.Operation;
import com.wpw.pim.repository.catalog.CategoryRepository;
import com.wpw.pim.repository.catalog.ProductGroupRepository;
import com.wpw.pim.repository.catalog.SectionRepository;
import com.wpw.pim.repository.operation.OperationRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.cutting.CuttingTypeNormalizer;
import com.wpw.pim.service.excel.dto.*;
import com.wpw.pim.service.excel.parser.WpwCatalogParser;
import com.wpw.pim.service.excel.report.ImportReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.cache.annotation.CacheEvict;
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
 * Импорт из формата WPW_Catalog_v3.xlsx (один лист, без SEO-колонок).
 *
 * Поток:
 *  1. validate() — разбор + валидация без записи в БД → ValidationReport
 *  2. execute()  — полный импорт → Markdown-отчёт
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WpwCatalogImportService {

    private final WpwCatalogParser            parser;
    private final CuttingTypeNormalizer        cuttingTypeNormalizer;
    private final ImportReportGenerator        reportGenerator;

    private final SectionRepository           sectionRepo;
    private final CategoryRepository          categoryRepo;
    private final ProductGroupRepository      groupRepo;
    private final ProductRepository           productRepo;
    private final ProductTranslationRepository translationRepo;
    private final OperationRepository          operationRepo;

    // -------------------------------------------------------------------------
    // Validate
    // -------------------------------------------------------------------------

    public ValidationReport validate(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(is)) {

            var sheet = wb.getSheet(WpwCatalogParser.SHEET_NAME);
            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Лист «" + WpwCatalogParser.SHEET_NAME + "» не найден. " +
                    "Убедитесь, что загружен файл формата WPW Catalog v3.");
            }

            var evaluator = wb.getCreationHelper().createFormulaEvaluator();
            List<WpwCatalogRow> rows    = parser.parse(sheet, evaluator);
            List<String>        unknown = parser.unknownHeaders(sheet);

            List<ValidationIssue> issues = new ArrayList<>();
            validateRows(rows, issues);

            long errors   = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count();
            long warnings = issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.WARNING).count();

            return ValidationReport.builder()
                .totalProductRows(rows.size())
                .totalGroupRows(0)
                .errorCount((int) errors)
                .warningCount((int) warnings)
                .canProceed(errors == 0)
                .issues(issues)
                .unknownHeaders(unknown)
                .build();
        }
    }

    private void validateRows(List<WpwCatalogRow> rows, List<ValidationIssue> issues) {
        Set<String> seenSkus = new HashSet<>();

        for (WpwCatalogRow r : rows) {
            int row = r.getRowNum();

            if (blank(r.getSku())) {
                issues.add(ValidationIssue.error(ValidationIssue.Sheet.PRODUCTS, row, "SKU", null,
                    "SKU отсутствует — строка будет пропущена"));
                continue;
            }

            if (!seenSkus.add(r.getSku())) {
                issues.add(ValidationIssue.warning(ValidationIssue.Sheet.PRODUCTS, row, "SKU", r.getSku(),
                    "Дублирующийся SKU в файле — будет использована последняя запись"));
            }

            if (blank(r.getCategory())) {
                issues.add(ValidationIssue.error(ValidationIssue.Sheet.PRODUCTS, row, "Category", null,
                    "Category не указана — товар не может быть импортирован"));
            }

            if (blank(r.getGroup())) {
                issues.add(ValidationIssue.error(ValidationIssue.Sheet.PRODUCTS, row, "Group", null,
                    "Group не указана — товар не может быть импортирован"));
            }

            if (blank(r.getDescriptionEn())) {
                issues.add(ValidationIssue.warning(ValidationIssue.Sheet.PRODUCTS, row, "Description_EN", null,
                    "Description_EN отсутствует"));
            }

            validateDecimal(issues, row, "D_mm",      r.getDMm());
            validateDecimal(issues, row, "D1_mm",     r.getD1Mm());
            validateDecimal(issues, row, "B_mm",      r.getBMm());
            validateDecimal(issues, row, "L_mm",      r.getLMm());
            validateDecimal(issues, row, "R_mm",      r.getRMm());
            validateDecimal(issues, row, "Angle_deg", r.getAngleDeg());
            validateDecimal(issues, row, "Shank_mm",  r.getShankMm());
            validateInteger(issues, row, "Flutes",    r.getFlutes());
        }
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @CacheEvict(value = {"operations", "categories"}, allEntries = true)
    @Transactional
    public String execute(MultipartFile file) throws Exception {
        Instant start = Instant.now();

        try (InputStream is = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(is)) {

            var sheet = wb.getSheet(WpwCatalogParser.SHEET_NAME);
            if (sheet == null) {
                throw new IllegalArgumentException("Лист «" + WpwCatalogParser.SHEET_NAME + "» не найден");
            }

            var evaluator        = wb.getCreationHelper().createFormulaEvaluator();
            List<WpwCatalogRow> rows = parser.parse(sheet, evaluator);

            StatsAccumulator acc = new StatsAccumulator();
            acc.totalProductRows = rows.size();

            Section defaultSection              = findOrCreateSection(acc);
            Map<String, Category>     catCache  = new HashMap<>();
            Map<String, ProductGroup> grpCache  = new HashMap<>();
            Map<String, String>       tagCache  = new HashMap<>(); // code → code (already ensured in DB)

            for (WpwCatalogRow r : rows) {
                if (blank(r.getSku()) || blank(r.getCategory()) || blank(r.getGroup())) {
                    acc.skipped++;
                    continue;
                }
                try {
                    Category cat = findOrCreateCategory(r.getCategory(), defaultSection, catCache, acc);
                    ProductGroup grp = findOrCreateGroup(r.getGroup(), r.getCategory(), cat, grpCache, acc);
                    importProduct(r, grp, tagCache, acc);
                } catch (Exception e) {
                    acc.errors.add("SKU " + r.getSku() + " (строка " + r.getRowNum() + "): " + e.getMessage());
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
        String name, Section section,
        Map<String, Category> cache, StatsAccumulator acc
    ) {
        String slug = slugify(name);
        if (cache.containsKey(slug)) return cache.get(slug);

        Category cat = categoryRepo.findBySlug(slug).orElseGet(() -> {
            Category c = new Category();
            c.setSlug(slug);
            c.setSection(section);
            c.setTranslations(Map.of("en", name));
            c.setSortOrder(cache.size());
            acc.categoriesCreated++;
            return categoryRepo.save(c);
        });

        if (!cache.containsKey(slug)) acc.categoriesFound++;
        cache.put(slug, cat);
        return cat;
    }

    private ProductGroup findOrCreateGroup(
        String groupName, String categoryName,
        Category category,
        Map<String, ProductGroup> cache, StatsAccumulator acc
    ) {
        // Slug уникален в рамках категории: category-slug__group-slug
        String slug = slugify(categoryName) + "--" + slugify(groupName);
        if (cache.containsKey(slug)) return cache.get(slug);

        ProductGroup grp = groupRepo.findBySlug(slug).orElseGet(() -> {
            ProductGroup pg = new ProductGroup();
            pg.setSlug(slug);
            pg.setGroupCode(slugify(groupName));
            pg.setTranslations(Map.of("en", groupName));
            pg.setCategory(category);
            pg.setSortOrder(cache.size());
            acc.groupsCreated++;
            return groupRepo.save(pg);
        });

        if (!cache.containsKey(slug)) acc.groupsFound++;
        cache.put(slug, grp);
        return grp;
    }

    // -------------------------------------------------------------------------
    // Товары
    // -------------------------------------------------------------------------

    private void importProduct(WpwCatalogRow r, ProductGroup group,
                               Map<String, String> tagCache, StatsAccumulator acc) {
        boolean isNew = !productRepo.existsByToolNo(r.getSku());

        Product product = productRepo.findByToolNo(r.getSku()).orElseGet(Product::new);
        product.setToolNo(r.getSku());
        product.setProductType(parseProductType(r.getProductType()));
        product.setStatus(ProductStatus.active);
        product.setGroup(group);

        product.setToolMaterials(splitPipe(r.getToolMaterial()));
        product.setWorkpieceMaterials(splitPipe(r.getWorkpieceMaterial()));
        product.setMachineTypes(splitPipe(r.getMachineType()));

        // Application Tags: comma-separated → operationCodes + auto-create missing tags
        Set<String> tagCodes = resolveTagCodes(r.getApplicationTags(), tagCache);
        product.setOperationCodes(tagCodes);

        ProductAttributes attr = product.getAttributes();
        if (attr == null) {
            attr = new ProductAttributes();
            attr.setProduct(product);
            product.setAttributes(attr);
        }
        fillAttributes(attr, r);

        product = productRepo.save(product);

        // Normalize tags for display: replace underscores with spaces, keep comma-separated
        String displayTags = normalizeTagsForDisplay(r.getApplicationTags());
        upsertTranslation(product, "en", r.getDescriptionEn(), displayTags, r.getAiDescriptionEn());
        if (!blank(r.getNameRu())) {
            upsertTranslation(product, "ru", r.getNameRu(), null, null);
        }

        if (isNew) acc.created++;
        else acc.updated++;
    }

    /**
     * Парсит строку тегов через запятую, для каждого тега находит или создаёт
     * запись в таблице operations. Возвращает набор кодов.
     * <p>
     * Пример: "Cove Cutting, Decorative Profiling, Grooving"
     * → codes: {"cove-cutting", "decorative-profiling", "grooving"}
     * </p>
     */
    private Set<String> resolveTagCodes(String raw, Map<String, String> tagCache) {
        if (blank(raw)) return new HashSet<>();

        Set<String> codes = new HashSet<>();
        // Support both comma and pipe separators (pipe was the legacy format)
        String[] parts = raw.split("[,|]");
        int sortBase = (int) operationRepo.count();

        for (String part : parts) {
            String name = part.trim().replace('_', ' ');
            if (name.isEmpty()) continue;

            String code = slugify(name);
            if (code.isEmpty()) continue;
            if (code.length() > 30) code = code.substring(0, 30).replaceAll("-$", "");

            if (!tagCache.containsKey(code)) {
                if (!operationRepo.existsById(code)) {
                    Operation op = new Operation();
                    op.setCode(code);
                    op.setName(name);
                    op.setNameKey("op." + code);
                    op.setSortOrder(sortBase++);
                    operationRepo.save(op);
                }
                tagCache.put(code, code);
            }
            codes.add(code);
        }
        return codes;
    }

    private void fillAttributes(ProductAttributes attr, WpwCatalogRow r) {
        attr.setDMm(decimal(r.getDMm()));
        attr.setD1Mm(decimal(r.getD1Mm()));
        attr.setBMm(decimal(r.getBMm()));
        attr.setLMm(decimal(r.getLMm()));
        attr.setRMm(decimal(r.getRMm()));
        attr.setAngleDeg(decimal(r.getAngleDeg()));
        attr.setShankMm(decimal(r.getShankMm()));
        attr.setShankInch(r.getShankInch());
        attr.setFlutes(shortVal(r.getFlutes()));
        if (!blank(r.getCuttingType())) {
            attr.setCuttingType(cuttingTypeNormalizer.normalize(r.getCuttingType()));
        }
    }

    private void upsertTranslation(Product product, String locale,
                                   String name, String applications, String aiDescription) {
        if (blank(name)) return;

        ProductTranslationId tid = new ProductTranslationId(product.getId(), locale);
        ProductTranslation t = translationRepo.findById(tid).orElseGet(() -> {
            ProductTranslation tr = new ProductTranslation();
            tr.setId(tid);
            tr.setProduct(product);
            return tr;
        });

        t.setName(name);
        if (!blank(applications)) t.setApplications(applications);
        if (!blank(aiDescription)) {
            t.setLongDescription(aiDescription);
            t.setAiGenerated(true);
        }
        translationRepo.save(t);
    }

    // -------------------------------------------------------------------------
    // Утилиты
    // -------------------------------------------------------------------------

    private static ProductType parseProductType(String raw) {
        if (raw == null) return ProductType.main;
        return switch (raw.trim().toLowerCase()) {
            case "spare part", "spare_part" -> ProductType.spare_part;
            case "accessory"                -> ProductType.accessory;
            default                         -> ProductType.main;
        };
    }

    /** Нормализует строку тегов для хранения в translation.applications: убирает подчёркивания. */
    private static String normalizeTagsForDisplay(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Arrays.stream(raw.split("[,|]"))
            .map(s -> s.trim().replace('_', ' '))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(", "));
    }

    private static Set<String> splitPipe(String raw) {
        if (raw == null || raw.isBlank()) return new HashSet<>();
        return Arrays.stream(raw.split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(HashSet::new));
    }

    private static String slugify(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }

    private static BigDecimal decimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private static Short shortVal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Excel returns integers as "2.0" — try direct parse first, then via double
            try { return Short.parseShort(s); } catch (NumberFormatException ignored) {}
            double d = Double.parseDouble(s);
            if (d == Math.floor(d) && !Double.isInfinite(d)) return (short) (long) d;
            return null;
        } catch (NumberFormatException e) { return null; }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static final java.util.regex.Pattern RANGE_PATTERN =
        java.util.regex.Pattern.compile("^\\d+(\\.\\d+)?-\\d+(\\.\\d+)?$");

    private static void validateDecimal(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null) return;
        try {
            new BigDecimal(value);
        } catch (NumberFormatException e) {
            String msg = RANGE_PATTERN.matcher(value).matches()
                ? "Диапазонное значение «" + value + "» — будет сохранено как null"
                : "Нечисловое значение «" + value + "» — будет сохранено как null";
            issues.add(ValidationIssue.warning(ValidationIssue.Sheet.PRODUCTS, row, field, value, msg));
        }
    }

    private static void validateInteger(List<ValidationIssue> issues, int row, String field, String value) {
        if (value == null) return;
        try {
            Integer.parseInt(value);
            return; // valid integer string
        } catch (NumberFormatException ignored) {}
        // Excel stores integers as "2.0" — accept whole-number doubles without warning
        try {
            double d = Double.parseDouble(value);
            if (d == Math.floor(d) && !Double.isInfinite(d)) return; // fine
        } catch (NumberFormatException ignored) {}
        issues.add(ValidationIssue.warning(ValidationIssue.Sheet.PRODUCTS, row, field, value,
            "Нечисловое значение «" + value + "» — будет сохранено как null"));
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
