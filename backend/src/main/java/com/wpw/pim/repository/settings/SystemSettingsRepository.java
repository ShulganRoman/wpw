package com.wpw.pim.repository.settings;

import com.wpw.pim.domain.settings.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
}
