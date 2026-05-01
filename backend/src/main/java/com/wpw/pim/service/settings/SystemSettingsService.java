package com.wpw.pim.service.settings;

import com.wpw.pim.domain.settings.SystemSettings;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.settings.SystemSettingsRepository;
import com.wpw.pim.security.DealerPrincipal;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemSettingsRepository repository;
    private final DealerRepository dealerRepository;

    @Transactional(readOnly = true)
    public SystemSettingsDto get() {
        SystemSettings s = load();
        return new SystemSettingsDto(
            s.isRequireImagesAdmin(), s.isRequireImagesDealer(), s.isRequireImagesPublic(),
            s.isRequirePriceAdmin(),  s.isRequirePriceDealer(),  s.isRequirePricePublic()
        );
    }

    @Transactional
    public SystemSettingsDto update(SystemSettingsDto dto) {
        SystemSettings s = load();
        s.setRequireImagesAdmin(dto.requireImagesAdmin());
        s.setRequireImagesDealer(dto.requireImagesDealer());
        s.setRequireImagesPublic(dto.requireImagesPublic());
        s.setRequirePriceAdmin(dto.requirePriceAdmin());
        s.setRequirePriceDealer(dto.requirePriceDealer());
        s.setRequirePricePublic(dto.requirePricePublic());
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
        if (auth == null || !auth.isAuthenticated()) return s.isRequireImagesPublic();
        if (isAdminRole(auth)) return s.isRequireImagesAdmin();
        boolean isDealer = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_DEALER"));
        if (isDealer) return s.isRequireImagesDealer();
        return s.isRequireImagesPublic();
    }

    @Transactional(readOnly = true)
    public boolean shouldRequirePrice(Authentication auth) {
        SystemSettings s = load();
        if (auth == null || !auth.isAuthenticated()) return s.isRequirePricePublic();
        if (isAdminRole(auth)) return s.isRequirePriceAdmin();
        boolean isDealer = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_DEALER"));
        if (isDealer) return s.isRequirePriceDealer();
        return s.isRequirePricePublic();
    }

    /**
     * Returns the dealer's price list ID from the current auth principal, or null if not a dealer.
     * Used to scope the "has price" filter to the specific dealer's price list.
     */
    @Transactional(readOnly = true)
    public UUID getDealerPriceListId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof DealerPrincipal dp) {
            var pl = dp.getDealer().getPriceList();
            return pl != null ? pl.getId() : null;
        }
        if (principal instanceof UserDetails ud) {
            return dealerRepository.findByUserUsername(ud.getUsername())
                .map(d -> d.getPriceList() != null ? d.getPriceList().getId() : null)
                .orElse(null);
        }
        return null;
    }

    private SystemSettings load() {
        return repository.findById(1L).orElseGet(() -> {
            SystemSettings s = new SystemSettings();
            return repository.save(s);
        });
    }
}
