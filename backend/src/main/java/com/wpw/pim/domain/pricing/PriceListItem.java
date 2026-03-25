package com.wpw.pim.domain.pricing;

import com.wpw.pim.domain.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "price_list_items")
@Getter @Setter @NoArgsConstructor
public class PriceListItem {

    @EmbeddedId
    private PriceListItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("priceListId")
    @JoinColumn(name = "price_list_id")
    private PriceList priceList;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal price;
}
