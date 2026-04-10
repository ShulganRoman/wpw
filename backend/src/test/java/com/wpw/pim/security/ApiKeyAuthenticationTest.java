package com.wpw.pim.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticationTest {

    @Test
    @DisplayName("unauthenticated token holds API key as credentials")
    void unauthenticated_holdsApiKey() {
        ApiKeyAuthentication auth = new ApiKeyAuthentication("my-key");

        assertThat(auth.getCredentials()).isEqualTo("my-key");
        assertThat(auth.getPrincipal()).isNull();
        assertThat(auth.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("authenticated token holds principal and authorities")
    void authenticated_holdsPrincipalAndAuthorities() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_DEALER"));
        ApiKeyAuthentication auth = new ApiKeyAuthentication("principal-obj", authorities);

        assertThat(auth.getPrincipal()).isEqualTo("principal-obj");
        assertThat(auth.getCredentials()).isNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).hasSize(1);
    }
}
