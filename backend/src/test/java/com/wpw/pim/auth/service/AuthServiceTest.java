package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.dto.LoginRequest;
import com.wpw.pim.auth.dto.LoginResponse;
import com.wpw.pim.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link AuthService}.
 * Проверяют логику аутентификации: валидные/невалидные credentials,
 * disabled user, генерацию JWT-токена.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login -- успешная аутентификация возвращает токен и привилегии")
    void login_validCredentials_returnsLoginResponse() {
        Role role = createRole("ADMIN", Privilege.MODIFY_PRODUCTS, Privilege.BULK_IMPORT);
        User user = createUser("admin", "hashed-pw", role, true);

        when(userRepository.findByUsernameWithRole("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-pw")).thenReturn(true);
        when(jwtService.generateToken("admin")).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.privileges()).containsExactlyInAnyOrder("MODIFY_PRODUCTS", "BULK_IMPORT");

        verify(jwtService).generateToken("admin");
    }

    @Test
    @DisplayName("login -- несуществующий пользователь вызывает IllegalArgumentException")
    void login_userNotFound_throwsException() {
        when(userRepository.findByUsernameWithRole("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");

        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("login -- неверный пароль вызывает IllegalArgumentException")
    void login_wrongPassword_throwsException() {
        Role role = createRole("ADMIN");
        User user = createUser("admin", "hashed-pw", role, true);

        when(userRepository.findByUsernameWithRole("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("login -- disabled пользователь вызывает IllegalArgumentException")
    void login_disabledUser_throwsException() {
        Role role = createRole("ADMIN");
        User user = createUser("admin", "hashed-pw", role, false);

        when(userRepository.findByUsernameWithRole("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    // --- Helper methods ---

    private Role createRole(String name, Privilege... privileges) {
        Set<Privilege> privSet = privileges.length > 0
                ? EnumSet.copyOf(Set.of(privileges))
                : EnumSet.noneOf(Privilege.class);
        Role role = new Role(name, false, privSet);
        role.setId(1L);
        return role;
    }

    private User createUser(String username, String passwordHash, Role role, boolean enabled) {
        User user = new User(username, passwordHash, role);
        user.setId(1L);
        user.setEnabled(enabled);
        return user;
    }
}
