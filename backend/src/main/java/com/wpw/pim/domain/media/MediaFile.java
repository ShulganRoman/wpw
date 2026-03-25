package com.wpw.pim.domain.media;

import com.wpw.pim.domain.catalog.Category;
import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.enums.FileType;
import com.wpw.pim.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_files")
@Getter @Setter @NoArgsConstructor
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ProductGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType = FileType.image;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
