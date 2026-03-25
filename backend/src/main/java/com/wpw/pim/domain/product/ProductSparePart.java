package com.wpw.pim.domain.product;

import com.wpw.pim.domain.enums.PartRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "product_spare_parts")
@Getter @Setter @NoArgsConstructor
@IdClass(ProductSparePart.SparePartId.class)
public class ProductSparePart {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Product part;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_role", nullable = false, length = 20)
    private PartRole partRole;

    public static class SparePartId implements Serializable {
        private UUID product;
        private UUID part;
    }
}
