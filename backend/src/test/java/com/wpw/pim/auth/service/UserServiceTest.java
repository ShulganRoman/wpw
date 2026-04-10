package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.dto.UserRequest;
import com.wpw.pim.auth.dto.UserResponse;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link UserService}.
 * Покрывают CRUD-операции, валидацию уникальности username, роли.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final Role testRole = createRole(1L, "ADMIN");

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("возвращает список пользователей")
        void findAll_returnsUserResponses() {
            User user = createUser(1L, "admin", testRole);
            when(userRepository.findAll()).thenReturn(List.of(user));

            List<UserResponse> result = userService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).username()).isEqualTo("admin");
            assertThat(result.get(0).roleName()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("возвращает пустой список если нет пользователей")
        void findAll_empty_returnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("создаёт пользователя успешно")
        void create_validRequest_returnsUserResponse() {
            UserRequest request = new UserRequest("newuser", "pass123", 1L, true);

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });

            UserResponse response = userService.create(request);

            assertThat(response.username()).isEqualTo("newuser");
            assertThat(response.roleId()).isEqualTo(1L);
            verify(passwordEncoder).encode("pass123");
        }

        @Test
        @DisplayName("бросает исключение при дублировании username")
        void create_duplicateUsername_throwsException() {
            UserRequest request = new UserRequest("admin", "pass123", 1L, null);
            when(userRepository.existsByUsername("admin")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("бросает исключение если пароль пустой")
        void create_blankPassword_throwsException() {
            UserRequest request = new UserRequest("newuser", "   ", 1L, null);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password is required");
        }

        @Test
        @DisplayName("бросает исключение если пароль null")
        void create_nullPassword_throwsException() {
            UserRequest request = new UserRequest("newuser", null, 1L, null);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password is required");
        }

        @Test
        @DisplayName("бросает исключение если роль не найдена")
        void create_roleNotFound_throwsException() {
            UserRequest request = new UserRequest("newuser", "pass123", 999L, null);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("обновляет пользователя успешно")
        void update_validRequest_returnsUpdatedUser() {
            User existing = createUser(1L, "admin", testRole);
            UserRequest request = new UserRequest("admin-updated", "newpass", 1L, false);

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByUsername("admin-updated")).thenReturn(false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.update(1L, request);

            assertThat(response.username()).isEqualTo("admin-updated");
            assertThat(response.enabled()).isFalse();
        }

        @Test
        @DisplayName("не обновляет пароль если он null")
        void update_nullPassword_doesNotUpdatePassword() {
            User existing = createUser(1L, "admin", testRole);
            UserRequest request = new UserRequest("admin", null, 1L, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.update(1L, request);

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("бросает исключение если пользователь не найден")
        void update_userNotFound_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(999L, new UserRequest("u", "p", 1L, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("бросает исключение при смене username на занятый")
        void update_duplicateUsername_throwsException() {
            User existing = createUser(1L, "admin", testRole);
            UserRequest request = new UserRequest("taken", "pass", 1L, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> userService.update(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет пользователя успешно")
        void delete_existingUser_deletesSuccessfully() {
            User existing = createUser(1L, "admin", testRole);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

            userService.delete(1L);

            verify(userRepository).delete(existing);
        }

        @Test
        @DisplayName("бросает исключение если пользователь не найден")
        void delete_userNotFound_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // --- Helper methods ---

    private static Role createRole(Long id, String name) {
        Role role = new Role(name, false, EnumSet.noneOf(Privilege.class));
        role.setId(id);
        return role;
    }

    private static User createUser(Long id, String username, Role role) {
        User user = new User(username, "hashed", role);
        user.setId(id);
        return user;
    }
}
