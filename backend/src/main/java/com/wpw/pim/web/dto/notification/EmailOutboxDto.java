package com.wpw.pim.web.dto.notification;

import com.wpw.pim.domain.notification.EmailOutboxStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmailOutboxDto(
    UUID id,
    String recipients,
    String subject,
    EmailOutboxStatus status,
    int attempts,
    String lastError,
    boolean hasAttachment,
    String attachmentFilename,
    OffsetDateTime createdAt,
    OffsetDateTime nextAttemptAt,
    OffsetDateTime sentAt
) {}
