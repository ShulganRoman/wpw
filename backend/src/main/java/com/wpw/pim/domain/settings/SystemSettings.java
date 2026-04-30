package com.wpw.pim.domain.settings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor
public class SystemSettings {

    @Id
    private Long id = 1L;

    @Column(name = "require_images_admin", nullable = false)
    private boolean requireImagesAdmin = false;

    @Column(name = "require_images_dealer", nullable = false)
    private boolean requireImagesDealer = false;

    @Column(name = "require_images_public", nullable = false)
    private boolean requireImagesPublic = false;
}
