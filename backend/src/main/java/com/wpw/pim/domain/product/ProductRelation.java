package com.wpw.pim.domain.product;

import com.wpw.pim.domain.enums.RelationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_relations")
@Getter @Setter @NoArgsConstructor
public class ProductRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_product_id", nullable = false)
    private Product fromProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_product_id", nullable = false)
    private Product toProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;
}
