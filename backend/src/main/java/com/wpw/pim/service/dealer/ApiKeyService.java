package com.wpw.pim.service.dealer;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    private final SecureRandom random = new SecureRandom();

    public String generateKey() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashKey(String rawKey) {
        return encoder.encode(rawKey);
    }

    public boolean verifyKey(String rawKey, String hash) {
        return encoder.matches(rawKey, hash);
    }
}
