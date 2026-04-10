package com.wpw.pim.service.dealer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link ApiKeyService}.
 * Проверяют генерацию, хеширование и верификацию API-ключей.
 */
class ApiKeyServiceTest {

    private final ApiKeyService apiKeyService = new ApiKeyService();

    @Test
    @DisplayName("generateKey -- возвращает непустой Base64-URL ключ")
    void generateKey_returnsNonEmptyBase64UrlKey() {
        String key = apiKeyService.generateKey();

        assertThat(key).isNotBlank();
        // Base64 URL-safe: только [A-Za-z0-9_-]
        assertThat(key).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("generateKey -- каждый вызов генерирует уникальный ключ")
    void generateKey_producesUniqueKeys() {
        String key1 = apiKeyService.generateKey();
        String key2 = apiKeyService.generateKey();

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("hashKey -- возвращает BCrypt хеш")
    void hashKey_returnsBCryptHash() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);

        assertThat(hash).startsWith("$2a$");
        assertThat(hash).hasSize(60);
    }

    @Test
    @DisplayName("verifyKey -- успешно верифицирует правильный ключ")
    void verifyKey_correctKey_returnsTrue() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);

        assertThat(apiKeyService.verifyKey(key, hash)).isTrue();
    }

    @Test
    @DisplayName("verifyKey -- отклоняет неправильный ключ")
    void verifyKey_wrongKey_returnsFalse() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);
        String wrongKey = apiKeyService.generateKey();

        assertThat(apiKeyService.verifyKey(wrongKey, hash)).isFalse();
    }

    @Test
    @DisplayName("generateKey -- длина ключа соответствует 32 байтам в Base64")
    void generateKey_hasExpectedLength() {
        String key = apiKeyService.generateKey();

        // 32 bytes in Base64 URL without padding: ceil(32*4/3) = 43 chars
        assertThat(key).hasSize(43);
    }
}
