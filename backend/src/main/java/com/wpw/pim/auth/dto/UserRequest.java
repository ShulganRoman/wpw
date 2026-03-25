package com.wpw.pim.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @Size(min = 4, message = "Password must be at least 4 characters")
        String password,

        @NotNull(message = "Role ID is required")
        Long roleId,

        Boolean enabled
) {}
