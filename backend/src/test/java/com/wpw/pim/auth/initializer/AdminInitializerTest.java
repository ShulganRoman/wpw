package com.wpw.pim.auth.initializer;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminInitializerTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminInitializer adminInitializer;

    @Test
    @DisplayName("creates admin role with all privileges when not exists")
    void run_createsAdminRole() throws Exception {
        Role adminRole = new Role("admin", true, EnumSet.allOf(Privilege.class));
        adminRole.setId(1L);

        when(roleRepository.findByName("admin")).thenReturn(Optional.empty());
        when(roleRepository.findByName("dealer")).thenReturn(Optional.empty());
        when(roleRepository.findByName("user")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminInitializer.run();

        verify(roleRepository, atLeast(3)).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updates admin role if missing privileges")
    void run_updatesAdminRolePrivileges() throws Exception {
        Role adminRole = new Role("admin", true, EnumSet.of(Privilege.MODIFY_PRODUCTS));
        adminRole.setId(1L);
        Role dealerRole = new Role("dealer", true, EnumSet.of(Privilege.BULK_EXPORT, Privilege.MODIFY_PRODUCTS));
        Role userRole = new Role("user", true, EnumSet.noneOf(Privilege.class));

        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("dealer")).thenReturn(Optional.of(dealerRole));
        when(roleRepository.findByName("user")).thenReturn(Optional.of(userRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        adminInitializer.run();

        // Admin role should be updated to include ALL privileges
        assertThat(adminRole.getPrivileges()).isEqualTo(EnumSet.allOf(Privilege.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("skips user creation when admin already exists")
    void run_skipsUserCreation_whenExists() throws Exception {
        Role adminRole = new Role("admin", true, EnumSet.allOf(Privilege.class));
        adminRole.setId(1L);

        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("dealer")).thenReturn(Optional.of(
                new Role("dealer", true, EnumSet.of(Privilege.BULK_EXPORT, Privilege.MODIFY_PRODUCTS))));
        when(roleRepository.findByName("user")).thenReturn(Optional.of(
                new Role("user", true, EnumSet.noneOf(Privilege.class))));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        adminInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
