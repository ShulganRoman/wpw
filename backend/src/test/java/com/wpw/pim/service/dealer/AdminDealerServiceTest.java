package com.wpw.pim.service.dealer;

import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerContact;
import com.wpw.pim.domain.pricing.PriceList;
import com.wpw.pim.repository.dealer.DealerContactRepository;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.web.dto.dealer.admin.DealerContactDto;
import com.wpw.pim.web.dto.dealer.admin.DealerContactSaveRequest;
import com.wpw.pim.web.dto.dealer.admin.DealerCreatedDto;
import com.wpw.pim.web.dto.dealer.admin.DealerDto;
import com.wpw.pim.web.dto.dealer.admin.DealerSaveRequest;
import com.wpw.pim.web.dto.dealer.admin.PasswordResetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link AdminDealerService}.
 * Покрывают создание/обновление/удаление дилеров, сброс пароля, управление контактами.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminDealerServiceTest {

    @Mock private DealerRepository dealerRepo;
    @Mock private DealerContactRepository contactRepo;
    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminDealerService service;

    private Role dealerRole;

    @BeforeEach
    void setup() {
        dealerRole = new Role();
        dealerRole.setName("dealer");
    }

    private DealerSaveRequest baseRequest() {
        return new DealerSaveRequest(
            "DEAL-01", "ACME LLC", "US",
            "Acme", "distributor", null, "CA", "SF", "Addr", "94100",
            "https://acme.com", true, "https://shop.acme.com",
            "logo.png", null, "USD", "GOLD", "notes", true, List.of()
        );
    }

    private DealerSaveRequest requestWithContacts() {
        return new DealerSaveRequest(
            "DEAL-02", "Beta Co", "DE",
            null, null, null, null, null, null, null,
            null, false, null, null, null, "EUR", null, null, true,
            List.of(new DealerContactSaveRequest("John", "manager", "j@x", "+1", true))
        );
    }

    @Nested
    @DisplayName("listAll & getById")
    class ListAndGet {

        @Test
        @DisplayName("listAll возвращает всех дилеров с контактами")
        void listAll() {
            UUID id = UUID.randomUUID();
            Dealer d = new Dealer();
            d.setId(id);
            d.setDealerCode("DEAL-1");
            d.setCompanyName("Co");

            when(dealerRepo.findAll()).thenReturn(List.of(d));
            when(contactRepo.findByDealerId(id)).thenReturn(List.of());

            List<DealerDto> result = service.listAll();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).dealerCode()).isEqualTo("DEAL-1");
        }

        @Test
        @DisplayName("getById возвращает DTO с username и priceListId")
        void getById() {
            UUID id = UUID.randomUUID();
            Dealer d = new Dealer();
            d.setId(id);
            User u = new User();
            u.setUsername("user1");
            d.setUser(u);
            PriceList pl = new PriceList();
            pl.setId(UUID.randomUUID());
            d.setPriceList(pl);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(d));
            when(contactRepo.findByDealerId(id)).thenReturn(List.of());

            DealerDto dto = service.getById(id);
            assertThat(dto.username()).isEqualTo("user1");
            assertThat(dto.priceListId()).isNotNull();
        }

        @Test
        @DisplayName("getById -- 404 если не найден")
        void getById_notFound() {
            UUID id = UUID.randomUUID();
            when(dealerRepo.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("успешно создаёт дилера и юзера")
        void createsSuccessfully() {
            DealerSaveRequest req = baseRequest();
            when(userRepo.existsByUsername("deal-01")).thenReturn(false);
            when(roleRepo.findByName("dealer")).thenReturn(Optional.of(dealerRole));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            DealerCreatedDto result = service.create(req);

            assertThat(result.username()).isEqualTo("deal-01");
            assertThat(result.generatedPassword()).isNotBlank();
            verify(userRepo).save(any(User.class));
            verify(dealerRepo).save(any(Dealer.class));
        }

        @Test
        @DisplayName("создание с контактами")
        void createsWithContacts() {
            DealerSaveRequest req = requestWithContacts();
            when(userRepo.existsByUsername(any())).thenReturn(false);
            when(roleRepo.findByName("dealer")).thenReturn(Optional.of(dealerRole));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            DealerCreatedDto result = service.create(req);

            assertThat(result.dealer().contacts()).hasSize(1);
            verify(contactRepo, atLeastOnce()).save(any(DealerContact.class));
        }

        @Test
        @DisplayName("CONFLICT если username уже занят")
        void conflictUsername() {
            DealerSaveRequest req = baseRequest();
            when(userRepo.existsByUsername(any())).thenReturn(true);

            assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR если роль 'dealer' не найдена")
        void roleNotFound() {
            DealerSaveRequest req = baseRequest();
            when(userRepo.existsByUsername(any())).thenReturn(false);
            when(roleRepo.findByName("dealer")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("слаг username делается из dealerCode (нижний регистр + чистка)")
        void usernameSlug() {
            DealerSaveRequest req = new DealerSaveRequest(
                "Deal#01@US", "Co", "US",
                null, null, null, null, null, null, null,
                null, false, null, null, null, "USD", null, null, true,
                List.of()
            );
            when(userRepo.existsByUsername(any())).thenReturn(false);
            when(roleRepo.findByName("dealer")).thenReturn(Optional.of(dealerRole));
            when(passwordEncoder.encode(any())).thenReturn("h");

            DealerCreatedDto result = service.create(req);
            assertThat(result.username()).matches("[a-z0-9_-]+");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("обновляет дилера и контакты")
        void updates() {
            UUID id = UUID.randomUUID();
            Dealer existing = new Dealer();
            existing.setId(id);
            User u = new User();
            u.setUsername("u");
            u.setEnabled(true);
            existing.setUser(u);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(existing));

            DealerDto result = service.update(id, requestWithContacts());

            assertThat(result.contacts()).hasSize(1);
            verify(contactRepo).deleteByDealerId(id);
            verify(userRepo).save(u);
            assertThat(u.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("обновление без user'а пропускает sync")
        void updateNoUser() {
            UUID id = UUID.randomUUID();
            Dealer existing = new Dealer();
            existing.setId(id);
            existing.setUser(null);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(existing));

            service.update(id, baseRequest());
            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("404 если дилер не найден")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(dealerRepo.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(id, baseRequest()))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет дилера и связанного user'а")
        void deletes() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(id);
            User u = new User();
            dealer.setUser(u);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(dealer));

            service.delete(id);

            verify(dealerRepo).deleteById(id);
            verify(userRepo).delete(u);
        }

        @Test
        @DisplayName("удаление без user'а — без вызова userRepo.delete")
        void deleteNoUser() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(id);
            dealer.setUser(null);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(dealer));

            service.delete(id);

            verify(dealerRepo).deleteById(id);
            verify(userRepo, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("сбрасывает и возвращает новый пароль")
        void resets() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(id);
            User u = new User();
            u.setUsername("user1");
            dealer.setUser(u);

            when(dealerRepo.findById(id)).thenReturn(Optional.of(dealer));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            PasswordResetDto result = service.resetPassword(id);

            assertThat(result.username()).isEqualTo("user1");
            assertThat(result.newPassword()).isNotBlank();
            assertThat(u.getPasswordHash()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("400 если у дилера нет user'а")
        void noUser() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(id);
            dealer.setUser(null);
            when(dealerRepo.findById(id)).thenReturn(Optional.of(dealer));

            assertThatThrownBy(() -> service.resetPassword(id))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("contacts")
    class Contacts {

        @Test
        @DisplayName("addContact добавляет контакт")
        void add() {
            UUID id = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(id);
            when(dealerRepo.findById(id)).thenReturn(Optional.of(dealer));

            DealerContactSaveRequest req = new DealerContactSaveRequest(
                "Jane", "owner", "jane@x", "+1", false);

            DealerContactDto dto = service.addContact(id, req);
            assertThat(dto.contactName()).isEqualTo("Jane");
            verify(contactRepo).save(any(DealerContact.class));
        }

        @Test
        @DisplayName("updateContact обновляет контакт")
        void update() {
            UUID dealerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            DealerContact existing = new DealerContact();
            existing.setId(contactId);
            existing.setDealer(dealer);

            when(contactRepo.findById(contactId)).thenReturn(Optional.of(existing));

            DealerContactSaveRequest req = new DealerContactSaveRequest(
                "Updated", "ceo", "u@x", "+0", true);

            DealerContactDto dto = service.updateContact(dealerId, contactId, req);
            assertThat(dto.contactName()).isEqualTo("Updated");
            assertThat(dto.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("updateContact -- 404 если dealerId не совпадает")
        void updateMismatchedDealer() {
            UUID dealerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            Dealer otherDealer = new Dealer();
            otherDealer.setId(UUID.randomUUID());
            DealerContact existing = new DealerContact();
            existing.setId(contactId);
            existing.setDealer(otherDealer);

            when(contactRepo.findById(contactId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateContact(dealerId, contactId,
                new DealerContactSaveRequest("X", null, null, null, false)))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("deleteContact удаляет контакт")
        void delete() {
            UUID dealerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            Dealer dealer = new Dealer();
            dealer.setId(dealerId);
            DealerContact existing = new DealerContact();
            existing.setId(contactId);
            existing.setDealer(dealer);

            when(contactRepo.findById(contactId)).thenReturn(Optional.of(existing));

            service.deleteContact(dealerId, contactId);

            verify(contactRepo).delete(existing);
        }

        @Test
        @DisplayName("deleteContact -- 404 если контакт не существует")
        void deleteNotFound() {
            UUID dealerId = UUID.randomUUID();
            UUID contactId = UUID.randomUUID();
            when(contactRepo.findById(contactId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteContact(dealerId, contactId))
                .isInstanceOf(ResponseStatusException.class);
        }
    }
}
