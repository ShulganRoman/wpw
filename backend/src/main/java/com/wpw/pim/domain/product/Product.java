package com.wpw.pim.domain.product;

import com.wpw.pim.domain.catalog.ProductGroup;
import com.wpw.pim.domain.enums.ProductStatus;
import com.wpw.pim.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tool_no", nullable = false, unique = true, length = 50)
    private String toolNo;

    @Column(name = "alt_tool_no", length = 50)
    private String altToolNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ProductGroup group;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType = ProductType.main;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.active;

    @Column(name = "is_orderable")
    private boolean isOrderable = true;

    @Column(name = "catalog_page")
    private Short catalogPage;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductTranslation> translations;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProductAttributes attributes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tool_material", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "material")
    private Set<String> toolMaterials = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_workpiece_material", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "material")
    private Set<String> workpieceMaterials = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_machine_type", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "machine_type")
    private Set<String> machineTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_machine_brand", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "machine_brand")
    private Set<String> machineBrands = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_operations", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "operation_code")
    private Set<String> operationCodes = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
