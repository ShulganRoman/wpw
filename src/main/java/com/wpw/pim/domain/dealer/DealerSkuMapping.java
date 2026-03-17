package com.wpw.pim.domain.dealer;

import com.wpw.pim.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dealer_sku_mapping")
@Getter @Setter @NoArgsConstructor
public class DealerSkuMapping {

    @EmbeddedId
    private DealerSkuMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dealerId")
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "dealer_sku", nullable = false, length = 100)
    private String dealerSku;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
