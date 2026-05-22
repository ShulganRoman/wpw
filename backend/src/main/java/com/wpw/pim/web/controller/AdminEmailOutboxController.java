package com.wpw.pim.web.controller;

import com.wpw.pim.domain.notification.EmailOutbox;
import com.wpw.pim.domain.notification.EmailOutboxStatus;
import com.wpw.pim.repository.notification.EmailOutboxRepository;
import com.wpw.pim.service.email.EmailOutboxDispatcher;
import com.wpw.pim.web.dto.notification.EmailOutboxDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/email-outbox")
@PreAuthorize("hasAuthority('MANAGE_DEALERS')")
@RequiredArgsConstructor
@Tag(name = "Admin: Email Outbox", description = "Outgoing email queue (delivery status, manual retries)")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "MANAGE_DEALERS required")
})
public class AdminEmailOutboxController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final EmailOutboxRepository repository;
    private final EmailOutboxDispatcher dispatcher;

    @GetMapping
    @Operation(summary = "List outbox messages by status")
    @ApiResponse(responseCode = "200", description = "List of messages")
    public List<EmailOutboxDto> list(
        @RequestParam(defaultValue = "FAILED") EmailOutboxStatus status,
        @RequestParam(defaultValue = "100") int limit
    ) {
        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return repository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, capped))
            .stream().map(this::toDto).toList();
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry sending now")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Retry attempted (check status)"),
        @ApiResponse(responseCode = "404", description = "Outbox row not found")
    })
    public EmailOutboxDto retry(@PathVariable UUID id) {
        EmailOutbox row = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox row not found"));

        // Reopen FAILED rows and reset the retry timer so the dispatcher picks it up immediately.
        if (row.getStatus() != EmailOutboxStatus.SENT) {
            row.setStatus(EmailOutboxStatus.PENDING);
            row.setNextAttemptAt(OffsetDateTime.now());
            repository.save(row);
            dispatcher.tryDeliverAsync(row.getId());
        }
        return toDto(repository.findById(id).orElse(row));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an outbox row (e.g. obsolete failure)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }

    private EmailOutboxDto toDto(EmailOutbox e) {
        return new EmailOutboxDto(
            e.getId(),
            e.getRecipients(),
            e.getSubject(),
            e.getStatus(),
            e.getAttempts(),
            e.getLastError(),
            e.getAttachment() != null && e.getAttachment().length > 0,
            e.getAttachmentFilename(),
            e.getCreatedAt(),
            e.getNextAttemptAt(),
            e.getSentAt()
        );
    }
}
