package com.wpw.pim.domain.dealer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dealer_contacts")
@Getter @Setter @NoArgsConstructor
public class DealerContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column
    private String role;

    @Column
    private String email;

    @Column
    private String phone;

    @Column(name = "is_primary")
    private boolean isPrimary = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
