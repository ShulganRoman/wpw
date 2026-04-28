package com.wpw.pim.web.dto.dealer.admin;

import java.util.UUID;

public record DealerContactDto(
    UUID id,
    String contactName,
    String role,
    String email,
    String phone,
    boolean isPrimary
) {}
