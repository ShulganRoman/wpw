package com.wpw.pim.service.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpw.pim.domain.product.Product;
import com.wpw.pim.domain.product.ProductAttributes;
import com.wpw.pim.domain.product.ProductTranslation;
import com.wpw.pim.domain.product.ProductTranslationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLdServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonLdService jsonLdService = new JsonLdService(objectMapper);

    private Product createProduct(String toolNo) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setToolNo(toolNo);
        return p;
    }

    @Test
    @DisplayName("generates valid JSON-LD with all attributes")
    @SuppressWarnings("unchecked")
    void generateJsonLd_fullProduct() throws Exception {
        Product product = createProduct("WPW-001");

        ProductAttributes attrs = new ProductAttributes();
        attrs.setProduct(product);
        attrs.setDMm(BigDecimal.valueOf(12.5));
        attrs.setLMm(BigDecimal.valueOf(50));
        attrs.setShankMm(BigDecimal.valueOf(8));
        attrs.setFlutes((short) 2);
        attrs.setCuttingType("straight");
        attrs.setEan13("4260000000001");
        product.setAttributes(attrs);

        ProductTranslation translation = new ProductTranslation();
        translation.setId(new ProductTranslationId(product.getId(), "en"));
        translation.setProduct(product);
        translation.setName("Straight Router Bit");
        translation.setShortDescription("A high-quality straight bit");

        String jsonLd = jsonLdService.buildProductJsonLd(product, translation, "en");

        assertThat(jsonLd).isNotNull();
        Map<String, Object> parsed = objectMapper.readValue(jsonLd, Map.class);

        assertThat(parsed).containsEntry("@context", "https://schema.org");
        assertThat(parsed).containsEntry("@type", "Product");
        assertThat(parsed).containsEntry("sku", "WPW-001");
        assertThat(parsed).containsEntry("name", "Straight Router Bit");
        assertThat(parsed).containsEntry("description", "A high-quality straight bit");
        assertThat(parsed).containsEntry("gtin13", "4260000000001");
        assertThat(parsed).containsKey("additionalProperty");
        assertThat(parsed).containsKey("brand");
    }

    @Test
    @DisplayName("uses toolNo as name when translation is null")
    @SuppressWarnings("unchecked")
    void generateJsonLd_nullTranslation() throws Exception {
        Product product = createProduct("WPW-002");

        String jsonLd = jsonLdService.buildProductJsonLd(product, null, "en");

        assertThat(jsonLd).isNotNull();
        Map<String, Object> parsed = objectMapper.readValue(jsonLd, Map.class);
        assertThat(parsed).containsEntry("name", "WPW-002");
        assertThat(parsed).doesNotContainKey("description");
    }

    @Test
    @DisplayName("omits additionalProperty when no attributes")
    @SuppressWarnings("unchecked")
    void generateJsonLd_noAttributes() throws Exception {
        Product product = createProduct("WPW-003");
        ProductTranslation translation = new ProductTranslation();
        translation.setName("Test Product");

        String jsonLd = jsonLdService.buildProductJsonLd(product, translation, "en");

        assertThat(jsonLd).isNotNull();
        Map<String, Object> parsed = objectMapper.readValue(jsonLd, Map.class);
        assertThat(parsed).doesNotContainKey("additionalProperty");
        assertThat(parsed).doesNotContainKey("gtin13");
    }

    @Test
    @DisplayName("omits null attribute values from additionalProperty list")
    @SuppressWarnings("unchecked")
    void generateJsonLd_partialAttributes() throws Exception {
        Product product = createProduct("WPW-004");

        ProductAttributes attrs = new ProductAttributes();
        attrs.setProduct(product);
        attrs.setDMm(BigDecimal.valueOf(10));
        // All other attributes are null
        product.setAttributes(attrs);

        ProductTranslation translation = new ProductTranslation();
        translation.setName("Partial Product");

        String jsonLd = jsonLdService.buildProductJsonLd(product, translation, "en");

        Map<String, Object> parsed = objectMapper.readValue(jsonLd, Map.class);
        assertThat(parsed).containsKey("additionalProperty");
        var props = (java.util.List<Map<String, Object>>) parsed.get("additionalProperty");
        assertThat(props).hasSize(1);
        assertThat(props.get(0)).containsEntry("name", "D (mm)");
    }
}
