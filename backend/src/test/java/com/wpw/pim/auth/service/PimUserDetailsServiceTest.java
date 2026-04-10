package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PimUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private PimUserDetailsService pimUserDetailsService;

    private User createUser(String username, String roleName, Privilege... privileges) {
        Role role = new Role(roleName, true, privileges.length > 0 ? EnumSet.of(privileges[0], privileges) : EnumSet.noneOf(Privilege.class));
        role.setId(1L);
        User user = new User(username, "$2a$10$hash", role);
        user.setId(1L);
        user.setEnabled(true);
        return user;
    }

    @Test
    @DisplayName("loads user with role and privileges as authorities")
    void loadUserByUsername_returnsUserWithAuthorities() {
        User user = createUser("admin", "admin", Privilege.MODIFY_PRODUCTS, Privilege.BULK_IMPORT);
        when(userRepository.findByUsernameWithRole("admin")).thenReturn(Optional.of(user));

        UserDetails details = pimUserDetailsService.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority")
                .contains("ROLE_ADMIN", "MODIFY_PRODUCTS", "BULK_IMPORT");
    }

    @Test
    @DisplayName("throws UsernameNotFoundException when user not found")
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByUsernameWithRole("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pimUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
