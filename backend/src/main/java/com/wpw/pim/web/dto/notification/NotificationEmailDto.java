package com.wpw.pim.web.dto.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationEmailDto(
    UUID id,
    String email,
    boolean active,
    OffsetDateTime createdAt
) {}
