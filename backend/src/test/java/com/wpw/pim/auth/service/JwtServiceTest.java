package com.wpw.pim.auth.service;

import com.wpw.pim.auth.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link JwtService}.
 * Проверяют генерацию JWT-токена и извлечение username из токена.
 */
class JwtServiceTest {

    /** Секрет длиной >= 256 бит (32 символа) для HMAC-SHA256 */
    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long!!";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 час

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, EXPIRATION_MS);
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("generateToken -- возвращает непустой JWT-токен")
    void generateToken_validUsername_returnsNonEmptyToken() {
        String token = jwtService.generateToken("admin");

        assertThat(token).isNotBlank();
        // JWT формат: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername -- извлекает username из валидного токена")
    void extractUsername_validToken_returnsUsername() {
        String token = jwtService.generateToken("admin");

        Optional<String> username = jwtService.extractUsername(token);

        assertThat(username).isPresent().contains("admin");
    }

    @Test
    @DisplayName("extractUsername -- возвращает empty для невалидного токена")
    void extractUsername_invalidToken_returnsEmpty() {
        Optional<String> username = jwtService.extractUsername("invalid.token.here");

        assertThat(username).isEmpty();
    }

    @Test
    @DisplayName("extractUsername -- возвращает empty для пустой строки")
    void extractUsername_emptyToken_returnsEmpty() {
        Optional<String> username = jwtService.extractUsername("");

        assertThat(username).isEmpty();
    }

    @Test
    @DisplayName("extractUsername -- возвращает empty для null")
    void extractUsername_nullToken_returnsEmpty() {
        Optional<String> username = jwtService.extractUsername(null);

        assertThat(username).isEmpty();
    }

    @Test
    @DisplayName("extractUsername -- возвращает empty для токена подписанного другим ключом")
    void extractUsername_tokenWithDifferentKey_returnsEmpty() {
        // Генерируем токен другим сервисом с другим секретом
        JwtProperties otherProperties = new JwtProperties(
                "another-secret-key-must-be-at-least-256-bits-long!", EXPIRATION_MS);
        JwtService otherService = new JwtService(otherProperties);
        String token = otherService.generateToken("admin");

        Optional<String> username = jwtService.extractUsername(token);

        assertThat(username).isEmpty();
    }

    @Test
    @DisplayName("extractUsername -- возвращает empty для просроченного токена")
    void extractUsername_expiredToken_returnsEmpty() {
        // Создаём сервис с нулевым TTL — токен истечёт мгновенно
        JwtProperties expiredProperties = new JwtProperties(SECRET, 0L);
        JwtService expiredService = new JwtService(expiredProperties);
        String token = expiredService.generateToken("admin");

        Optional<String> username = jwtService.extractUsername(token);

        assertThat(username).isEmpty();
    }

    @Test
    @DisplayName("generateToken -- разные username дают разные токены")
    void generateToken_differentUsernames_produceDifferentTokens() {
        String token1 = jwtService.generateToken("user1");
        String token2 = jwtService.generateToken("user2");

        assertThat(token1).isNotEqualTo(token2);
    }
}
