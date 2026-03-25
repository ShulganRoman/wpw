package com.wpw.pim.auth.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        Long roleId,
        String roleName,
        boolean enabled,
        LocalDateTime createdAt
) {}
