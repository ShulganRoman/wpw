package com.wpw.pim.service.export;

import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductTranslationRepository;
import com.wpw.pim.service.product.ProductFilterSpec;
import com.wpw.pim.web.dto.product.ProductFilter;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ProductRepository productRepo;
    private final ProductTranslationRepository translationRepo;

    private static final String[] HEADERS = {
        "tool_no", "alt_tool_no", "name", "short_description",
        "d_mm", "l_mm", "shank_mm", "flutes", "cutting_type",
        "has_ball_bearing", "ean13", "hs_code", "weight_g",
        "stock_status", "product_type", "status"
    };

    @Transactional(readOnly = true)
    public byte[] export(String format, String locale,
                         List<String> toolMaterial, List<String> workpieceMaterial,
                         BigDecimal dMmMin, BigDecimal dMmMax) {
        ProductFilter filter = new ProductFilter(locale, null, toolMaterial, workpieceMaterial,
            null, null, null, dMmMin, dMmMax, null, null, null, null, 1, 10_000);
        List<Product> products = productRepo.findAll(new ProductFilterSpec(filter));

        List<UUID> ids = products.stream().map(Product::getId).toList();
        Map<UUID, ProductTranslation> translations = translationRepo.findByProductIdsAndLocale(ids, locale)
            .stream().collect(Collectors.toMap(t -> t.getId().getProductId(), t -> t));

        try {
            return switch (format.toLowerCase()) {
                case "xlsx" -> exportXlsx(products, translations);
                case "xml"  -> exportXml(products, translations);
                default     -> exportCsv(products, translations);
            };
        } catch (Exception e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    private byte[] exportCsv(List<Product> products, Map<UUID, ProductTranslation> translations) throws Exception {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(HEADERS);
            for (Product p : products) {
                ProductTranslation t = translations.get(p.getId());
                ProductAttributes a = p.getAttributes();
                writer.writeNext(toRow(p, t, a));
            }
        }
        return sw.toString().getBytes();
    }

    private byte[] exportXlsx(List<Product> products, Map<UUID, ProductTranslation> translations) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Products");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowNum = 1;
            for (Product p : products) {
                ProductTranslation t = translations.get(p.getId());
                ProductAttributes a = p.getAttributes();
                Row row = sheet.createRow(rowNum++);
                String[] data = toRow(p, t, a);
                for (int i = 0; i < data.length; i++) {
                    row.createCell(i).setCellValue(data[i] != null ? data[i] : "");
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportXml(List<Product> products, Map<UUID, ProductTranslation> translations) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<products>\n");
        for (Product p : products) {
            ProductTranslation t = translations.get(p.getId());
            ProductAttributes a = p.getAttributes();
            sb.append("  <product>\n");
            sb.append("    <tool_no>").append(p.getToolNo()).append("</tool_no>\n");
            if (t != null) sb.append("    <name>").append(escape(t.getName())).append("</name>\n");
            if (a != null && a.getDMm() != null) sb.append("    <d_mm>").append(a.getDMm()).append("</d_mm>\n");
            if (a != null && a.getLMm() != null) sb.append("    <l_mm>").append(a.getLMm()).append("</l_mm>\n");
            sb.append("    <status>").append(p.getStatus()).append("</status>\n");
            sb.append("  </product>\n");
        }
        sb.append("</products>");
        return sb.toString().getBytes();
    }

    private String[] toRow(Product p, ProductTranslation t, ProductAttributes a) {
        return new String[]{
            p.getToolNo(),
            p.getAltToolNo(),
            t != null ? t.getName() : "",
            t != null ? t.getShortDescription() : "",
            a != null && a.getDMm() != null ? a.getDMm().toPlainString() : "",
            a != null && a.getLMm() != null ? a.getLMm().toPlainString() : "",
            a != null && a.getShankMm() != null ? a.getShankMm().toPlainString() : "",
            a != null && a.getFlutes() != null ? a.getFlutes().toString() : "",
            a != null ? a.getCuttingType() : "",
            a != null ? String.valueOf(a.isHasBallBearing()) : "",
            a != null ? a.getEan13() : "",
            a != null ? a.getHsCode() : "",
            a != null && a.getWeightG() != null ? a.getWeightG().toString() : "",
            a != null && a.getStockStatus() != null ? a.getStockStatus().name() : "",
            p.getProductType().name(),
            p.getStatus().name()
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
