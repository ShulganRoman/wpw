package com.wpw.pim.web.controller;

import com.wpw.pim.service.dealer.AdminDealerService;
import com.wpw.pim.web.dto.dealer.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dealers")
@PreAuthorize("hasAuthority('MANAGE_DEALERS')")
@RequiredArgsConstructor
@Tag(name = "Admin: Dealers", description = "Управление дилерами и их контактами. Требует MANAGE_DEALERS.")
public class AdminDealerController {

    private final AdminDealerService service;

    @GetMapping
    @Operation(summary = "Список дилеров")
    public List<DealerDto> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить дилера по ID")
    public DealerDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать дилера", description = "Создаёт пользователя с ролью DEALER и генерирует временный пароль.")
    public DealerCreatedDto create(@Valid @RequestBody DealerSaveRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить дилера")
    public DealerDto update(@PathVariable UUID id, @Valid @RequestBody DealerSaveRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить дилера")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Сбросить пароль дилера", description = "Генерирует новый временный пароль и возвращает его в ответе.")
    public PasswordResetDto resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }

    // --- contacts sub-resource ---

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить контакт дилера")
    public DealerContactDto addContact(@PathVariable UUID id,
                                       @Valid @RequestBody DealerContactSaveRequest req) {
        return service.addContact(id, req);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Обновить контакт дилера")
    public DealerContactDto updateContact(@PathVariable UUID id,
                                          @PathVariable UUID contactId,
                                          @Valid @RequestBody DealerContactSaveRequest req) {
        return service.updateContact(id, contactId, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить контакт дилера")
    public void deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        service.deleteContact(id, contactId);
    }
}
