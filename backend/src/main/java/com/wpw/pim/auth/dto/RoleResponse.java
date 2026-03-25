package com.wpw.pim.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record RoleResponse(
        Long id,
        String name,
        boolean builtIn,
        Set<String> privileges,
        LocalDateTime createdAt
) {}
