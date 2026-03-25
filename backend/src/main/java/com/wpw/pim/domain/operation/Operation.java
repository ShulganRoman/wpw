package com.wpw.pim.domain.operation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "operations")
@Getter @Setter @NoArgsConstructor
public class Operation {

    @Id
    private String code;

    @Column(name = "name_key", nullable = false)
    private String nameKey;

    @Column(name = "sort_order")
    private int sortOrder;
}
