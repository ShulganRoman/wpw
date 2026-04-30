package com.wpw.pim.service.settings;

import com.wpw.pim.domain.settings.SystemSettings;
import com.wpw.pim.repository.settings.SystemSettingsRepository;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    @Transactional(readOnly = true)
    public SystemSettingsDto get() {
        SystemSettings s = load();
        return new SystemSettingsDto(s.isRequireImagesAdmin(), s.isRequireImagesDealer(), s.isRequireImagesPublic());
    }

    @Transactional
    public SystemSettingsDto update(SystemSettingsDto dto) {
        SystemSettings s = load();
        s.setRequireImagesAdmin(dto.requireImagesAdmin());
        s.setRequireImagesDealer(dto.requireImagesDealer());
        s.setRequireImagesPublic(dto.requireImagesPublic());
        repository.save(s);
        return dto;
    }

    public boolean isAdminRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().noneMatch(
            a -> a.getAuthority().equals("ROLE_DEALER")
              || a.getAuthority().equals("ROLE_USER")
              || a.getAuthority().equals("ROLE_ANONYMOUS")
        );
    }

    @Transactional(readOnly = true)
    public boolean shouldRequireImages(Authentication auth) {
        SystemSettings s = load();
        if (auth == null || !auth.isAuthenticated()) {
            return s.isRequireImagesPublic();
        }
        if (isAdminRole(auth)) return s.isRequireImagesAdmin();
        boolean isDealer = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_DEALER"));
        if (isDealer) return s.isRequireImagesDealer();
        return s.isRequireImagesPublic();
    }

    private SystemSettings load() {
        return repository.findById(1L).orElseGet(() -> {
            SystemSettings s = new SystemSettings();
            return repository.save(s);
        });
    }
}
