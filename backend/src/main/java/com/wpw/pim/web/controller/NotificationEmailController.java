package com.wpw.pim.web.controller;

import com.wpw.pim.domain.notification.NotificationEmail;
import com.wpw.pim.repository.notification.NotificationEmailRepository;
import com.wpw.pim.web.dto.notification.NotificationEmailDto;
import com.wpw.pim.web.dto.notification.NotificationEmailSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notification-emails")
@PreAuthorize("hasAuthority('MANAGE_DEALERS')")
@RequiredArgsConstructor
@Tag(name = "Admin: Notification Emails", description = "Адреса для email-уведомлений о заказах")
public class NotificationEmailController {

    private final NotificationEmailRepository repository;

    @GetMapping
    @Operation(summary = "Список адресов")
    public List<NotificationEmailDto> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить адрес")
    public NotificationEmailDto create(@Valid @RequestBody NotificationEmailSaveRequest req) {
        NotificationEmail entity = new NotificationEmail();
        entity.setEmail(req.email());
        entity.setActive(req.active());
        return toDto(repository.save(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить адрес")
    public NotificationEmailDto update(@PathVariable UUID id, @Valid @RequestBody NotificationEmailSaveRequest req) {
        NotificationEmail entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        entity.setEmail(req.email());
        entity.setActive(req.active());
        return toDto(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить адрес")
    public void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }

    private NotificationEmailDto toDto(NotificationEmail e) {
        return new NotificationEmailDto(e.getId(), e.getEmail(), e.isActive(), e.getCreatedAt());
    }
}
