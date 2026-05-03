package com.wpw.pim.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Authentication response with JWT token")
public record LoginResponse(
        @Schema(description = "JWT Bearer token")
        String token,
        @Schema(description = "Username")
        String username,
        @Schema(description = "Role name")
        String role,
        @Schema(description = "Set of permission codes")
        Set<String> privileges
) {}
