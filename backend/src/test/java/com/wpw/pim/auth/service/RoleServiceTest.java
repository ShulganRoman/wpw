package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.dto.RoleRequest;
import com.wpw.pim.auth.dto.RoleResponse;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link RoleService}.
 * Покрывают CRUD ролей, валидацию привилегий, защиту встроенных ролей.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("возвращает все роли")
        void findAll_returnsRoleResponses() {
            Role role = createRole(1L, "ADMIN", false, Privilege.MODIFY_PRODUCTS);
            when(roleRepository.findAll()).thenReturn(List.of(role));

            List<RoleResponse> result = roleService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("ADMIN");
            assertThat(result.get(0).privileges()).contains("MODIFY_PRODUCTS");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("возвращает роль по id")
        void findById_existingId_returnsRole() {
            Role role = createRole(1L, "ADMIN", false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            RoleResponse response = roleService.findById(1L);

            assertThat(response.name()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("бросает исключение если роль не найдена")
        void findById_notFound_throwsException() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("создаёт роль с привилегиями")
        void create_validRequest_returnsRoleResponse() {
            RoleRequest request = new RoleRequest("EDITOR", Set.of("MODIFY_PRODUCTS", "BULK_IMPORT"));

            when(roleRepository.existsByName("EDITOR")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setId(2L);
                return r;
            });

            RoleResponse response = roleService.create(request);

            assertThat(response.name()).isEqualTo("EDITOR");
            assertThat(response.privileges()).containsExactlyInAnyOrder("MODIFY_PRODUCTS", "BULK_IMPORT");
        }

        @Test
        @DisplayName("создаёт роль с пустыми привилегиями")
        void create_emptyPrivileges_createsRoleSuccessfully() {
            RoleRequest request = new RoleRequest("VIEWER", Set.of());

            when(roleRepository.existsByName("VIEWER")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setId(3L);
                return r;
            });

            RoleResponse response = roleService.create(request);

            assertThat(response.privileges()).isEmpty();
        }

        @Test
        @DisplayName("бросает исключение при дублировании имени")
        void create_duplicateName_throwsException() {
            when(roleRepository.existsByName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> roleService.create(new RoleRequest("ADMIN", Set.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("бросает исключение при неизвестной привилегии")
        void create_unknownPrivilege_throwsException() {
            when(roleRepository.existsByName("BAD_ROLE")).thenReturn(false);

            assertThatThrownBy(() -> roleService.create(new RoleRequest("BAD_ROLE", Set.of("NONEXISTENT"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown privilege");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("обновляет роль успешно")
        void update_validRequest_updatesRole() {
            Role existing = createRole(1L, "EDITOR", false);
            RoleRequest request = new RoleRequest("EDITOR_V2", Set.of("BULK_EXPORT"));

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.existsByName("EDITOR_V2")).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            RoleResponse response = roleService.update(1L, request);

            assertThat(response.name()).isEqualTo("EDITOR_V2");
            assertThat(response.privileges()).contains("BULK_EXPORT");
        }

        @Test
        @DisplayName("не проверяет уникальность если имя не изменилось")
        void update_sameName_noUniquenessCheck() {
            Role existing = createRole(1L, "EDITOR", false);
            RoleRequest request = new RoleRequest("EDITOR", Set.of("BULK_EXPORT"));

            when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            RoleResponse response = roleService.update(1L, request);

            assertThat(response.name()).isEqualTo("EDITOR");
            verify(roleRepository, never()).existsByName(anyString());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет роль успешно")
        void delete_customRole_deletesSuccessfully() {
            Role role = createRole(1L, "CUSTOM", false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(userRepository.existsByRoleId(1L)).thenReturn(false);

            roleService.delete(1L);

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("бросает исключение при удалении встроенной роли")
        void delete_builtInRole_throwsException() {
            Role role = createRole(1L, "ADMIN", true);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            assertThatThrownBy(() -> roleService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete built-in role");
        }

        @Test
        @DisplayName("бросает исключение если роль назначена пользователям")
        void delete_roleAssignedToUsers_throwsException() {
            Role role = createRole(1L, "EDITOR", false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(userRepository.existsByRoleId(1L)).thenReturn(true);

            assertThatThrownBy(() -> roleService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("assigned to users");
        }

        @Test
        @DisplayName("бросает исключение если роль не найдена")
        void delete_notFound_throwsException() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    // --- Helpers ---

    private static Role createRole(Long id, String name, boolean builtIn, Privilege... privileges) {
        Set<Privilege> privSet = privileges.length > 0
                ? EnumSet.copyOf(Set.of(privileges))
                : EnumSet.noneOf(Privilege.class);
        Role role = new Role(name, builtIn, privSet);
        role.setId(id);
        return role;
    }
}
