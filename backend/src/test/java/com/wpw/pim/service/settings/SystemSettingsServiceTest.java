package com.wpw.pim.service.settings;

import com.wpw.pim.domain.settings.SystemSettings;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.settings.SystemSettingsRepository;
import com.wpw.pim.web.dto.settings.SystemSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link SystemSettingsService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemSettingsServiceTest {

    @Mock
    private SystemSettingsRepository repository;

    @Mock
    private DealerRepository dealerRepository;

    private SystemSettingsService service;

    @BeforeEach
    void setUp() {
        service = new SystemSettingsService(repository, dealerRepository);
    }

    private SystemSettings settings(boolean admin, boolean dealer, boolean publicFlag) {
        SystemSettings s = new SystemSettings();
        s.setRequireImagesAdmin(admin);
        s.setRequireImagesDealer(dealer);
        s.setRequireImagesPublic(publicFlag);
        return s;
    }

    private Authentication authWithAuthorities(boolean authenticated, String... authorities) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(authenticated);
        List<GrantedAuthority> list = java.util.Arrays.stream(authorities)
            .map(SimpleGrantedAuthority::new)
            .map(a -> (GrantedAuthority) a)
            .toList();
        doReturn(list).when(auth).getAuthorities();
        return auth;
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("get -- loads singleton row and maps it to DTO")
        void get_loadsSingletonAndMapsToDto() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(true, false, true)));

            SystemSettingsDto dto = service.get();

            assertThat(dto.requireImagesAdmin()).isTrue();
            assertThat(dto.requireImagesDealer()).isFalse();
            assertThat(dto.requireImagesPublic()).isTrue();
        }

        @Test
        @DisplayName("get -- if no row exists, creates default with all false")
        void get_whenMissing_createsDefaultAllFalse() {
            when(repository.findById(1L)).thenReturn(Optional.empty());
            when(repository.save(any(SystemSettings.class))).thenAnswer(inv -> inv.getArgument(0));

            SystemSettingsDto dto = service.get();

            assertThat(dto.requireImagesAdmin()).isFalse();
            assertThat(dto.requireImagesDealer()).isFalse();
            assertThat(dto.requireImagesPublic()).isFalse();
            verify(repository).save(any(SystemSettings.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("update -- saves all three flags")
        void update_savesAllThreeFields() {
            SystemSettings existing = settings(false, false, false);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(SystemSettings.class))).thenAnswer(inv -> inv.getArgument(0));

            SystemSettingsDto input = new SystemSettingsDto(true, true, false, false, false, false);
            SystemSettingsDto returned = service.update(input);

            ArgumentCaptor<SystemSettings> captor = ArgumentCaptor.forClass(SystemSettings.class);
            verify(repository).save(captor.capture());
            SystemSettings saved = captor.getValue();
            assertThat(saved.isRequireImagesAdmin()).isTrue();
            assertThat(saved.isRequireImagesDealer()).isTrue();
            assertThat(saved.isRequireImagesPublic()).isFalse();
            assertThat(returned).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("isAdminRole")
    class IsAdminRole {

        @Test
        @DisplayName("isAdminRole(null) -- false")
        void isAdminRole_null_false() {
            assertThat(service.isAdminRole(null)).isFalse();
        }

        @Test
        @DisplayName("isAdminRole(unauthenticated) -- false")
        void isAdminRole_unauthenticated_false() {
            Authentication auth = authWithAuthorities(false);
            assertThat(service.isAdminRole(auth)).isFalse();
        }

        @Test
        @DisplayName("isAdminRole(ROLE_DEALER) -- false")
        void isAdminRole_dealer_false() {
            Authentication auth = authWithAuthorities(true, "ROLE_DEALER");
            assertThat(service.isAdminRole(auth)).isFalse();
        }

        @Test
        @DisplayName("isAdminRole(ROLE_USER) -- false")
        void isAdminRole_user_false() {
            Authentication auth = authWithAuthorities(true, "ROLE_USER");
            assertThat(service.isAdminRole(auth)).isFalse();
        }

        @Test
        @DisplayName("isAdminRole(ROLE_ANONYMOUS) -- false")
        void isAdminRole_anonymous_false() {
            Authentication auth = authWithAuthorities(true, "ROLE_ANONYMOUS");
            assertThat(service.isAdminRole(auth)).isFalse();
        }

        @Test
        @DisplayName("isAdminRole(MANAGE_CATALOG) -- true")
        void isAdminRole_manageCatalog_true() {
            Authentication auth = authWithAuthorities(true, "MANAGE_CATALOG");
            assertThat(service.isAdminRole(auth)).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldRequireImages")
    class ShouldRequireImages {

        @Test
        @DisplayName("shouldRequireImages(null) -- returns publicFlag")
        void shouldRequireImages_null_returnsPublic() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(false, false, true)));

            assertThat(service.shouldRequireImages(null)).isTrue();
        }

        @Test
        @DisplayName("shouldRequireImages(unauthenticated) -- returns publicFlag")
        void shouldRequireImages_unauthenticated_returnsPublic() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(false, false, true)));
            Authentication auth = authWithAuthorities(false);

            assertThat(service.shouldRequireImages(auth)).isTrue();
        }

        @Test
        @DisplayName("shouldRequireImages(ROLE_DEALER) -- returns dealerFlag")
        void shouldRequireImages_dealer_returnsDealer() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(false, true, false)));
            Authentication auth = authWithAuthorities(true, "ROLE_DEALER");

            assertThat(service.shouldRequireImages(auth)).isTrue();
        }

        @Test
        @DisplayName("shouldRequireImages(ROLE_USER) -- returns publicFlag")
        void shouldRequireImages_userRole_returnsPublic() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(false, false, true)));
            Authentication auth = authWithAuthorities(true, "ROLE_USER");

            assertThat(service.shouldRequireImages(auth)).isTrue();
        }

        @Test
        @DisplayName("shouldRequireImages(admin) -- returns adminFlag")
        void shouldRequireImages_admin_returnsAdmin() {
            when(repository.findById(1L)).thenReturn(Optional.of(settings(true, false, false)));
            Authentication auth = authWithAuthorities(true, "MANAGE_CATALOG");

            assertThat(service.shouldRequireImages(auth)).isTrue();
        }
    }
}
