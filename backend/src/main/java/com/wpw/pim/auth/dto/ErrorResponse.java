package com.wpw.pim.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple error response as required: {"error": "message"}.
 */
@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error message", example = "Invalid credentials")
        String error
) {}
