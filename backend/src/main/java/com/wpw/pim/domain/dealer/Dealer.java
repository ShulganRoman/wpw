package com.wpw.pim.domain.dealer;

import com.wpw.pim.domain.pricing.PriceList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dealers")
@Getter @Setter @NoArgsConstructor
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2)
    private String country;

    @Column(name = "default_locale", length = 10)
    private String defaultLocale = "en";

    @Column(name = "api_key_hash", nullable = false, length = 60)
    private String apiKeyHash;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id")
    private PriceList priceList;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
