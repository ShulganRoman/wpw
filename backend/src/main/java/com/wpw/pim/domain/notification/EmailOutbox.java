package com.wpw.pim.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_outbox")
@Getter @Setter @NoArgsConstructor
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Comma-separated list of recipient addresses. */
    @Column(nullable = false, columnDefinition = "text")
    private String recipients;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(columnDefinition = "bytea")
    private byte[] attachment;

    @Column(name = "attachment_filename", length = 500)
    private String attachmentFilename;

    @Column(name = "attachment_mime", length = 200)
    private String attachmentMime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailOutboxStatus status = EmailOutboxStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt = OffsetDateTime.now();

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
