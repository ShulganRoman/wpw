package com.wpw.pim.auth.dto;

/**
 * Simple error response as required: {"error": "message"}.
 */
public record ErrorResponse(String error) {}
