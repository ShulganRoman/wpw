package com.wpw.pim.service.export;

import com.wpw.pim.domain.media.MediaFile;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.repository.media.MediaFileRepository;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.product.ProductFilterSpec;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис экспорта товаров в форматы CSV, XLSX и XML.
 * <p>
 * Формирует выгрузку с атрибутами товаров, переводами и URL изображений.
 * Изображения загружаются одним batch-запросом для избежания проблемы N+1.
 * </p>
 */
@Service
public class ExportService {

    private final ProductRepository productRepo;
    private final ProductTranslationRepository translationRepo;
    private final MediaFileRepository mediaFileRepo;
    private final String exportBaseUrl;

    public ExportService(ProductRepository productRepo,
                         ProductTranslationRepository translationRepo,
                         MediaFileRepository mediaFileRepo,
                         @Value("${pim.export.base-url}") String exportBaseUrl) {
        this.productRepo = productRepo;
        this.translationRepo = translationRepo;
        this.mediaFileRepo = mediaFileRepo;
        this.exportBaseUrl = exportBaseUrl;
    }

    private static final String[] HEADERS = {
        "tool_no", "alt_tool_no", "name", "short_description",
        "d_mm", "l_mm", "shank_mm", "flutes", "cutting_type",
        "has_ball_bearing", "ean13", "hs_code", "weight_g",
        "stock_status", "product_type", "status"
    };

    @Transactional(readOnly = true)
    public byte[] export(String format, String locale, ProductFilter filter) {
        List<Product> products = productRepo.findAll(new ProductFilterSpec(filter));

        List<UUID> ids = products.stream().map(Product::getId).toList();
        Map<UUID, ProductTranslation> translations = translationRepo.findByProductIdsAndLocale(ids, locale)
            .stream().collect(Collectors.toMap(t -> t.getId().getProductId(), t -> t));

        // Загрузка изображений одним batch-запросом, группировка по product ID
        Map<UUID, List<MediaFile>> imagesByProduct = ids.isEmpty()
            ? Collections.emptyMap()
            : mediaFileRepo.findByProductIds(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getProduct().getId(), LinkedHashMap::new, Collectors.toList()));

        // Определяем максимальное количество изображений среди всех товаров
        int maxImages = imagesByProduct.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);

        try {
            return switch (format.toLowerCase()) {
                case "xlsx" -> exportXlsx(products, translations, imagesByProduct, maxImages);
                case "xml"  -> exportXml(products, translations, imagesByProduct);
                default     -> exportCsv(products, translations, imagesByProduct, maxImages);
            };
        } catch (Exception e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    /**
     * Формирует массив заголовков с динамическими колонками изображений.
     *
     * @param maxImages максимальное количество изображений среди всех товаров
     * @return массив заголовков, включая image_url_1, image_url_2, ...
     */
    private String[] buildHeaders(int maxImages) {
        String[] headers = Arrays.copyOf(HEADERS, HEADERS.length + maxImages);
        for (int i = 0; i < maxImages; i++) {
            headers[HEADERS.length + i] = "image_url_" + (i + 1);
        }
        return headers;
    }

    /**
     * Формирует полный URL изображения, объединяя базовый URL экспорта и относительный путь.
     *
     * @param mediaFile медиафайл с относительным URL
     * @return полный URL вида https://pim.a2agent.co.il/media/products/DR12345/1.webp
     */
    private String buildImageUrl(MediaFile mediaFile) {
        return exportBaseUrl + mediaFile.getUrl();
    }

    private byte[] exportCsv(List<Product> products, Map<UUID, ProductTranslation> translations,
                             Map<UUID, List<MediaFile>> imagesByProduct, int maxImages) throws Exception {
        String[] headers = buildHeaders(maxImages);
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(headers);
            for (Product p : products) {
                ProductTranslation t = translations.get(p.getId());
                ProductAttributes a = p.getAttributes();
                List<MediaFile> images = imagesByProduct.getOrDefault(p.getId(), Collections.emptyList());
                writer.writeNext(toRow(p, t, a, images, maxImages));
            }
        }
        return sw.toString().getBytes();
    }

    private byte[] exportXlsx(List<Product> products, Map<UUID, ProductTranslation> translations,
                              Map<UUID, List<MediaFile>> imagesByProduct, int maxImages) throws Exception {
        String[] headers = buildHeaders(maxImages);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Products");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (Product p : products) {
                ProductTranslation t = translations.get(p.getId());
                ProductAttributes a = p.getAttributes();
                List<MediaFile> images = imagesByProduct.getOrDefault(p.getId(), Collections.emptyList());
                Row row = sheet.createRow(rowNum++);
                String[] data = toRow(p, t, a, images, maxImages);
                for (int i = 0; i < data.length; i++) {
                    row.createCell(i).setCellValue(data[i] != null ? data[i] : "");
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportXml(List<Product> products, Map<UUID, ProductTranslation> translations,
                             Map<UUID, List<MediaFile>> imagesByProduct) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<products>\n");
        for (Product p : products) {
            ProductTranslation t = translations.get(p.getId());
            ProductAttributes a = p.getAttributes();
            List<MediaFile> images = imagesByProduct.getOrDefault(p.getId(), Collections.emptyList());
            sb.append("  <product>\n");
            sb.append("    <tool_no>").append(p.getToolNo()).append("</tool_no>\n");
            if (t != null) sb.append("    <name>").append(escape(t.getName())).append("</name>\n");
            if (a != null && a.getDMm() != null) sb.append("    <d_mm>").append(a.getDMm()).append("</d_mm>\n");
            if (a != null && a.getLMm() != null) sb.append("    <l_mm>").append(a.getLMm()).append("</l_mm>\n");
            sb.append("    <status>").append(p.getStatus()).append("</status>\n");
            if (!images.isEmpty()) {
                sb.append("    <images>\n");
                for (MediaFile img : images) {
                    sb.append("      <image>").append(escape(buildImageUrl(img))).append("</image>\n");
                }
                sb.append("    </images>\n");
            }
            sb.append("  </product>\n");
        }
        sb.append("</products>");
        return sb.toString().getBytes();
    }

    /**
     * Формирует строку данных для CSV/XLSX экспорта, включая URL изображений.
     *
     * @param p         товар
     * @param t         перевод товара (может быть null)
     * @param a         атрибуты товара (может быть null)
     * @param images    список изображений товара, отсортированный по sort_order
     * @param maxImages максимальное количество колонок изображений
     * @return массив строковых значений для записи в строку
     */
    private String[] toRow(Product p, ProductTranslation t, ProductAttributes a,
                           List<MediaFile> images, int maxImages) {
        String[] row = new String[HEADERS.length + maxImages];
        row[0]  = p.getToolNo();
        row[1]  = p.getAltToolNo();
        row[2]  = t != null ? t.getName() : "";
        row[3]  = t != null ? t.getShortDescription() : "";
        row[4]  = a != null && a.getDMm() != null ? a.getDMm().toPlainString() : "";
        row[5]  = a != null && a.getLMm() != null ? a.getLMm().toPlainString() : "";
        row[6]  = a != null && a.getShankMm() != null ? a.getShankMm().toPlainString() : "";
        row[7]  = a != null && a.getFlutes() != null ? a.getFlutes().toString() : "";
        row[8]  = a != null ? a.getCuttingType() : "";
        row[9]  = a != null ? String.valueOf(a.isHasBallBearing()) : "";
        row[10] = a != null ? a.getEan13() : "";
        row[11] = a != null ? a.getHsCode() : "";
        row[12] = a != null && a.getWeightG() != null ? a.getWeightG().toString() : "";
        row[13] = a != null && a.getStockStatus() != null ? a.getStockStatus().name() : "";
        row[14] = p.getProductType().name();
        row[15] = p.getStatus().name();

        // Добавляем URL изображений в колонки image_url_1, image_url_2, ...
        for (int i = 0; i < maxImages; i++) {
            row[HEADERS.length + i] = i < images.size() ? buildImageUrl(images.get(i)) : "";
        }
        return row;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
