package com.wpw.pim.web.dto.dealer.admin;

import jakarta.validation.constraints.NotBlank;

public record DealerContactSaveRequest(
    @NotBlank String contactName,
    String role,
    String email,
    String phone,
    boolean isPrimary
) {}
