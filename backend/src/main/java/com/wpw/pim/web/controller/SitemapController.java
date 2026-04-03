package com.wpw.pim.web.controller;

import com.wpw.pim.repository.product.ProductRepository;
import com.wpw.pim.repository.product.ProductSitemapProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ProductRepository productRepository;

    @Value("${pim.export.base-url}")
    private String baseUrl;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public String sitemap() {
        List<ProductSitemapProjection> products = productRepository.findAllForSitemap();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Главная страница
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/</loc>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // Страница каталога
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/catalog</loc>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");

        // Страницы продуктов
        for (ProductSitemapProjection product : products) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/product/")
               .append(encode(product.getToolNo())).append("</loc>\n");
            if (product.getUpdatedAt() != null) {
                xml.append("    <lastmod>")
                   .append(product.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                   .append("</lastmod>\n");
            }
            xml.append("    <priority>0.7</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private String encode(String toolNo) {
        return toolNo.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
