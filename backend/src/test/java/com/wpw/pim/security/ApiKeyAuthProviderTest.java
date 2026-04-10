package com.wpw.pim.security;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.service.dealer.ApiKeyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthProviderTest {

    @Mock private DealerRepository dealerRepository;
    @Mock private ApiKeyService apiKeyService;

    @InjectMocks private ApiKeyAuthProvider apiKeyAuthProvider;

    private Dealer createDealer() {
        Dealer dealer = new Dealer();
        dealer.setId(UUID.randomUUID());
        dealer.setName("TestDealer");
        dealer.setApiKeyHash("$2a$10$hash");
        dealer.setActive(true);
        return dealer;
    }

    @Test
    @DisplayName("authenticates valid API key")
    void authenticate_validKey_returnsAuthentication() {
        Dealer dealer = createDealer();
        ApiKeyAuthentication auth = new ApiKeyAuthentication("valid-key");
        when(dealerRepository.findAllByIsActiveTrue()).thenReturn(List.of(dealer));
        when(apiKeyService.verifyKey("valid-key", dealer.getApiKeyHash())).thenReturn(true);

        Authentication result = apiKeyAuthProvider.authenticate(auth);

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(DealerPrincipal.class);
    }

    @Test
    @DisplayName("throws BadCredentialsException for invalid key")
    void authenticate_invalidKey_throws() {
        Dealer dealer = createDealer();
        ApiKeyAuthentication auth = new ApiKeyAuthentication("bad-key");
        when(dealerRepository.findAllByIsActiveTrue()).thenReturn(List.of(dealer));
        when(apiKeyService.verifyKey("bad-key", dealer.getApiKeyHash())).thenReturn(false);

        assertThatThrownBy(() -> apiKeyAuthProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid API key");
    }

    @Test
    @DisplayName("throws BadCredentialsException for null key")
    void authenticate_nullKey_throws() {
        ApiKeyAuthentication auth = new ApiKeyAuthentication((String) null);

        assertThatThrownBy(() -> apiKeyAuthProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("API key missing");
    }

    @Test
    @DisplayName("throws BadCredentialsException for blank key")
    void authenticate_blankKey_throws() {
        ApiKeyAuthentication auth = new ApiKeyAuthentication("  ");

        assertThatThrownBy(() -> apiKeyAuthProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("API key missing");
    }

    @Test
    @DisplayName("supports ApiKeyAuthentication class")
    void supports_apiKeyAuthentication() {
        assertThat(apiKeyAuthProvider.supports(ApiKeyAuthentication.class)).isTrue();
    }

    @Test
    @DisplayName("does not support other authentication types")
    void supports_otherTypes_returnsFalse() {
        assertThat(apiKeyAuthProvider.supports(Authentication.class)).isFalse();
    }
}
