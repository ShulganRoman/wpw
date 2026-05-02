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
    @DisplayName("generateKey -- returns non-empty Base64-URL key")
    void generateKey_returnsNonEmptyBase64UrlKey() {
        String key = apiKeyService.generateKey();

        assertThat(key).isNotBlank();
        // Base64 URL-safe: только [A-Za-z0-9_-]
        assertThat(key).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("generateKey -- each call generates unique key")
    void generateKey_producesUniqueKeys() {
        String key1 = apiKeyService.generateKey();
        String key2 = apiKeyService.generateKey();

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("hashKey -- returns BCrypt hash")
    void hashKey_returnsBCryptHash() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);

        assertThat(hash).startsWith("$2a$");
        assertThat(hash).hasSize(60);
    }

    @Test
    @DisplayName("verifyKey -- successfully verifies correct key")
    void verifyKey_correctKey_returnsTrue() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);

        assertThat(apiKeyService.verifyKey(key, hash)).isTrue();
    }

    @Test
    @DisplayName("verifyKey -- rejects wrong key")
    void verifyKey_wrongKey_returnsFalse() {
        String key = apiKeyService.generateKey();
        String hash = apiKeyService.hashKey(key);
        String wrongKey = apiKeyService.generateKey();

        assertThat(apiKeyService.verifyKey(wrongKey, hash)).isFalse();
    }

    @Test
    @DisplayName("generateKey -- key length corresponds to 32 bytes in Base64")
    void generateKey_hasExpectedLength() {
        String key = apiKeyService.generateKey();

        // 32 bytes in Base64 URL without padding: ceil(32*4/3) = 43 chars
        assertThat(key).hasSize(43);
    }
}
