package com.wpw.pim.domain.pricing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currencies")
@Getter @Setter @NoArgsConstructor
public class Currency {

    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 5)
    private String symbol;

    @Column(name = "decimal_places")
    private short decimalPlaces = 2;

    @Column(name = "is_active")
    private boolean isActive = true;
}
