package com.wpw.pim.web.dto.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationEmailSaveRequest(
    @NotBlank @Email String email,
    boolean active
) {}
