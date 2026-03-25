package com.wpw.pim.domain.dealer;

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
public class DealerSkuMappingId implements Serializable {
    @Column(name = "dealer_id")
    private UUID dealerId;
    @Column(name = "product_id")
    private UUID productId;
}
