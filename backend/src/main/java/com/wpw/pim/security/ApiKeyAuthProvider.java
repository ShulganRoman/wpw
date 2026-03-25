package com.wpw.pim.security;

import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.service.dealer.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthProvider implements AuthenticationProvider {

    private final DealerRepository dealerRepository;
    private final ApiKeyService apiKeyService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String rawKey = (String) authentication.getCredentials();
        if (rawKey == null || rawKey.isBlank()) throw new BadCredentialsException("API key missing");

        return dealerRepository.findAllByIsActiveTrue().stream()
            .filter(d -> apiKeyService.verifyKey(rawKey, d.getApiKeyHash()))
            .findFirst()
            .map(dealer -> {
                DealerPrincipal principal = new DealerPrincipal(dealer);
                return (Authentication) new ApiKeyAuthentication(principal, principal.getAuthorities());
            })
            .orElseThrow(() -> new BadCredentialsException("Invalid API key"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthentication.class.isAssignableFrom(authentication);
    }
}
