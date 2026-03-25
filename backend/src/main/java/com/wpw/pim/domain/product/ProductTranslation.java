package com.wpw.pim.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_translations")
@Getter @Setter @NoArgsConstructor
public class ProductTranslation {

    @EmbeddedId
    private ProductTranslationId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "seo_title", length = 70)
    private String seoTitle;

    @Column(name = "seo_description", length = 170)
    private String seoDescription;

    @Column(name = "applications", columnDefinition = "TEXT")
    private String applications;

    @Column(name = "ai_generated")
    private boolean aiGenerated;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
}
