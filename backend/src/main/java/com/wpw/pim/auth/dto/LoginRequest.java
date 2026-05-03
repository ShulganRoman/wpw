package com.wpw.pim.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Schema(description = "Username", example = "admin")
        String username,

        @NotBlank(message = "Password is required")
        @Schema(description = "Password", example = "secret123")
        String password
) {}
