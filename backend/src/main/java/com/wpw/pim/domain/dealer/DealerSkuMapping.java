package com.wpw.pim.domain.dealer;

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

    @Column(name = "dealer_sku", nullable = false)
    private String dealerSku;

    @Column(name = "dealer_brand")
    private String dealerBrand;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
