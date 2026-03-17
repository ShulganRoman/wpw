package com.wpw.pim.service.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonLdService {

    private final ObjectMapper objectMapper;

    public String buildProductJsonLd(Product product, ProductTranslation translation, String locale) {
        try {
            Map<String, Object> jsonLd = new LinkedHashMap<>();
            jsonLd.put("@context", "https://schema.org");
            jsonLd.put("@type", "Product");
            jsonLd.put("sku", product.getToolNo());
            jsonLd.put("name", translation != null ? translation.getName() : product.getToolNo());
            if (translation != null && translation.getShortDescription() != null) {
                jsonLd.put("description", translation.getShortDescription());
            }
            jsonLd.put("brand", Map.of("@type", "Brand", "name", "WPW Professional Cutting Tools"));

            ProductAttributes attrs = product.getAttributes();
            if (attrs != null) {
                List<Map<String, Object>> additionalProps = new ArrayList<>();
                addProp(additionalProps, "D (mm)", attrs.getDMm());
                addProp(additionalProps, "L (mm)", attrs.getLMm());
                addProp(additionalProps, "Shank (mm)", attrs.getShankMm());
                addProp(additionalProps, "Flutes", attrs.getFlutes());
                addProp(additionalProps, "Cutting Type", attrs.getCuttingType());
                if (attrs.getEan13() != null) {
                    jsonLd.put("gtin13", attrs.getEan13());
                }
                if (!additionalProps.isEmpty()) {
                    jsonLd.put("additionalProperty", additionalProps);
                }
            }

            return objectMapper.writeValueAsString(jsonLd);
        } catch (Exception e) {
            log.warn("Failed to build JSON-LD for product {}", product.getToolNo(), e);
            return null;
        }
    }

    private void addProp(List<Map<String, Object>> list, String name, Object value) {
        if (value == null) return;
        list.add(Map.of(
            "@type", "PropertyValue",
            "name", name,
            "value", value.toString()
        ));
    }
}
