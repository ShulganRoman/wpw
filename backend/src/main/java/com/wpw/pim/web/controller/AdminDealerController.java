package com.wpw.pim.web.controller;

import com.wpw.pim.service.dealer.AdminDealerService;
import com.wpw.pim.web.dto.dealer.admin.*;
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
public class AdminDealerController {

    private final AdminDealerService service;

    @GetMapping
    public List<DealerDto> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public DealerDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealerCreatedDto create(@Valid @RequestBody DealerSaveRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public DealerDto update(@PathVariable UUID id, @Valid @RequestBody DealerSaveRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    public PasswordResetDto resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }

    // --- contacts sub-resource ---

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public DealerContactDto addContact(@PathVariable UUID id,
                                       @Valid @RequestBody DealerContactSaveRequest req) {
        return service.addContact(id, req);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    public DealerContactDto updateContact(@PathVariable UUID id,
                                          @PathVariable UUID contactId,
                                          @Valid @RequestBody DealerContactSaveRequest req) {
        return service.updateContact(id, contactId, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        service.deleteContact(id, contactId);
    }
}
