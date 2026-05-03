package com.wpw.pim.web.controller;

import com.wpw.pim.service.dealer.AdminDealerService;
import com.wpw.pim.web.dto.dealer.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "MANAGE_DEALERS required")
})
public class AdminDealerController {

    private final AdminDealerService service;

    @GetMapping
    @Operation(summary = "List dealers")
    @ApiResponse(responseCode = "200", description = "List of dealers")
    public List<DealerDto> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dealer by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dealer details"),
        @ApiResponse(responseCode = "404", description = "Dealer not found")
    })
    public DealerDto get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create dealer", description = "Creates a user with DEALER role and generates a temporary password.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dealer created with temp password"),
        @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    public DealerCreatedDto create(@Valid @RequestBody DealerSaveRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dealer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dealer updated"),
        @ApiResponse(responseCode = "404", description = "Dealer not found")
    })
    public DealerDto update(@PathVariable UUID id, @Valid @RequestBody DealerSaveRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dealer")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dealer deleted"),
        @ApiResponse(responseCode = "404", description = "Dealer not found")
    })
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset dealer password", description = "Generates a new temporary password and returns it in the response.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New temp password returned"),
        @ApiResponse(responseCode = "404", description = "Dealer not found")
    })
    public PasswordResetDto resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }

    // --- contacts sub-resource ---

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add dealer contact")
    @ApiResponse(responseCode = "201", description = "Contact added")
    public DealerContactDto addContact(@PathVariable UUID id,
                                       @Valid @RequestBody DealerContactSaveRequest req) {
        return service.addContact(id, req);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Update dealer contact")
    @ApiResponse(responseCode = "200", description = "Contact updated")
    public DealerContactDto updateContact(@PathVariable UUID id,
                                          @PathVariable UUID contactId,
                                          @Valid @RequestBody DealerContactSaveRequest req) {
        return service.updateContact(id, contactId, req);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dealer contact")
    @ApiResponse(responseCode = "204", description = "Contact deleted")
    public void deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        service.deleteContact(id, contactId);
    }
}
