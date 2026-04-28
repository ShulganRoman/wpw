package com.wpw.pim.domain.dealer;

import com.wpw.pim.auth.domain.User;
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

    // --- legacy portal fields (used by dealer API auth) ---

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "api_key_hash", nullable = false, length = 60)
    private String apiKeyHash;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "default_locale", length = 10)
    private String defaultLocale = "en";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id")
    private PriceList priceList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // --- admin-managed dealer directory fields ---

    @Column(name = "dealer_code", unique = true)
    private String dealerCode;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "dealer_type")
    private String dealerType;

    @Column(name = "private_label_brand")
    private String privateLabelBrand;

    @Column
    private String country;

    @Column
    private String region;

    @Column
    private String city;

    @Column
    private String address;

    @Column(name = "postal_code")
    private String postalCode;

    @Column
    private String website;

    @Column(name = "has_ecommerce")
    private boolean hasEcommerce = false;

    @Column(name = "shop_url")
    private String shopUrl;

    @Column
    private String logo;

    @Column
    private String currency;

    @Column(name = "discount_tier")
    private String discountTier;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
