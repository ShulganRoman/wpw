package com.wpw.pim.domain.product;

import com.wpw.pim.domain.enums.BoreType;
import com.wpw.pim.domain.enums.RotationDirection;
import com.wpw.pim.domain.enums.StockStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_attributes")
@Getter @Setter @NoArgsConstructor
public class ProductAttributes {

    @Id
    private UUID productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    // Геометрия
    @Column(name = "d_mm")
    private BigDecimal dMm;
    @Column(name = "d1_mm")
    private BigDecimal d1Mm;
    @Column(name = "d2_mm")
    private BigDecimal d2Mm;
    @Column(name = "b_mm")
    private BigDecimal bMm;
    @Column(name = "b1_mm")
    private BigDecimal b1Mm;
    @Column(name = "l_mm")
    private BigDecimal lMm;
    @Column(name = "l1_mm")
    private BigDecimal l1Mm;
    @Column(name = "r_mm")
    private BigDecimal rMm;
    @Column(name = "a_mm")
    private BigDecimal aMm;
    @Column(name = "angle_deg")
    private BigDecimal angleDeg;
    @Column(name = "shank_mm")
    private BigDecimal shankMm;

    @Column(name = "shank_inch", length = 10)
    private String shankInch;

    private Short flutes;
    @Column(name = "blade_no")
    private Short bladeNo;

    @Column(name = "cutting_type", length = 30)
    private String cuttingType;

    @Column(name = "has_ball_bearing")
    private boolean hasBallBearing;

    @Column(name = "ball_bearing_code", length = 100)
    private String ballBearingCode;

    @Column(name = "has_retainer")
    private boolean hasRetainer;

    @Column(name = "retainer_code", length = 50)
    private String retainerCode;

    @Column(name = "can_resharpen")
    private boolean canResharpen;

    @Enumerated(EnumType.STRING)
    @Column(name = "rotation_direction")
    private RotationDirection rotationDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "bore_type")
    private BoreType boreType;

    // E-commerce
    @Column(length = 13)
    private String ean13;

    @Column(length = 12)
    private String upc12;

    @Column(name = "custom_barcode", length = 50)
    private String customBarcode;

    @Column(name = "hs_code", length = 15)
    private String hsCode;

    @Column(name = "country_of_origin", length = 2)
    private String countryOfOrigin;

    @Column(name = "weight_g")
    private Integer weightG;

    @Column(name = "weight_gross_g")
    private Integer weightGrossG;

    @Column(name = "pkg_length_mm")
    private Short pkgLengthMm;

    @Column(name = "pkg_width_mm")
    private Short pkgWidthMm;

    @Column(name = "pkg_height_mm")
    private Short pkgHeightMm;

    @Column(name = "pkg_qty")
    private Short pkgQty = 1;

    @Column(name = "carton_qty")
    private Short cartonQty;

    // Stock
    @Column(name = "stock_qty")
    private Integer stockQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status")
    private StockStatus stockStatus;

    @Column(name = "stock_updated_at")
    private OffsetDateTime stockUpdatedAt;
}
