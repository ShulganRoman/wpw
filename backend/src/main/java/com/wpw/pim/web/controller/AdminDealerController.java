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
@Tag(name = "Admin: Dealers", description = "Dealer and contact management. Requires MANAGE_DEALERS.")
public class AdminDealerController {

    private final AdminDealerService service;

    @GetMapping
    @Operation(summary = "List dealers")
    public List<DealerDto> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dealer by ID")
    public DealerDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create dealer", description = "Creates a user with DEALER role and generates a temporary password.")
    public DealerCreatedDto create(@Valid @RequestBody DealerSaveRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dealer")
    public DealerDto update(@PathVariable UUID id, @Valid @RequestBody DealerSaveRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dealer")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset dealer password", description = "Generates a new temporary password and returns it in the response.")
    public PasswordResetDto resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }

    // --- contacts sub-resource ---

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add dealer contact")
    public DealerContactDto addContact(@PathVariable UUID id,
                                       @Valid @RequestBody DealerContactSaveRequest req) {
        return service.addContact(id, req);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Update dealer contact")
    public DealerContactDto updateContact(@PathVariable UUID id,
                                          @PathVariable UUID contactId,
                                          @Valid @RequestBody DealerContactSaveRequest req) {
        return service.updateContact(id, contactId, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dealer contact")
    public void deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        service.deleteContact(id, contactId);
    }
}
