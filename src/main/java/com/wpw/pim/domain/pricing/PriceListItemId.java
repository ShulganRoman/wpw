package com.wpw.pim.domain.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class PriceListItemId implements Serializable {
    @Column(name = "price_list_id")
    private UUID priceListId;
    @Column(name = "product_id")
    private UUID productId;
    @Column(name = "min_qty")
    private int minQty;
}
