package com.wpw.pim.web.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link GlobalExceptionHandler}.
 * Проверяют маппинг исключений на HTTP-ответы (ProblemDetail).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResponseStatus -- маппит ResponseStatusException на ProblemDetail")
    void handleResponseStatus_mapsCorrectly() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");

        ProblemDetail pd = handler.handleResponseStatus(ex);

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getDetail()).isEqualTo("Product not found");
    }

    @Test
    @DisplayName("handleResponseStatus -- CONFLICT возвращает 409")
    void handleResponseStatus_conflict_returns409() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Already exists");

        ProblemDetail pd = handler.handleResponseStatus(ex);

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getDetail()).isEqualTo("Already exists");
    }

    @Test
    @DisplayName("handleAccessDenied -- возвращает 403 с Access denied")
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ProblemDetail pd = handler.handleAccessDenied(ex);

        assertThat(pd.getStatus()).isEqualTo(403);
        assertThat(pd.getDetail()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("handleGeneral -- возвращает 500 для неожиданного исключения")
    void handleGeneral_returns500() {
        Exception ex = new RuntimeException("something broke");

        ProblemDetail pd = handler.handleGeneral(ex);

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getDetail()).isEqualTo("Internal server error");
    }

    @Test
    @DisplayName("handleResponseStatus -- BAD_REQUEST возвращает 400")
    void handleResponseStatus_badRequest_returns400() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        ProblemDetail pd = handler.handleResponseStatus(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getDetail()).isEqualTo("Invalid input");
    }
}
