package com.wpw.pim.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_privileges",
            joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "privilege", length = 50)
    @Enumerated(EnumType.STRING)
    private Set<Privilege> privileges = new HashSet<>();

    public Role(String name, boolean builtIn, Set<Privilege> privileges) {
        this.name = name;
        this.builtIn = builtIn;
        this.privileges = privileges;
        this.createdAt = LocalDateTime.now();
    }
}
