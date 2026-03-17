package com.wpw.pim.auth.dto;

import java.util.Set;

public record LoginResponse(
        String token,
        String username,
        String role,
        Set<String> privileges
) {}
