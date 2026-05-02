package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.Dealer;
import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.security.DealerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DealerSkuResolverServiceTest {

    @Mock private DealerSkuMappingRepository mappingRepo;
    @Mock private DealerRepository dealerRepo;

    @InjectMocks private DealerSkuResolverService service;

    private final UUID dealerId = UUID.randomUUID();

    private Authentication authAsDealer(UUID id) {
        Dealer dealer = new Dealer();
        dealer.setId(id);
        DealerPrincipal principal = mock(DealerPrincipal.class);
        when(principal.getDealer()).thenReturn(dealer);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }

    private Authentication authAsAdmin() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("admin");
        when(auth.getAuthorities()).thenAnswer(inv ->
            List.of(new SimpleGrantedAuthority("MANAGE_CATALOG")));
        return auth;
    }

    private DealerSkuMapping mapping(UUID dId, String wpwSku, String dealerSku) {
        DealerSkuMapping m = new DealerSkuMapping();
        m.setId(new DealerSkuMappingId(dId, wpwSku));
        m.setDealerSku(dealerSku);
        return m;
    }

    @Nested
    @DisplayName("resolveBatch")
    class ResolveBatch {

        @Test
        @DisplayName("returns mapping for dealer by toolNos list")
        void returnsMappingForDealer() {
            Authentication auth = authAsDealer(dealerId);
            List<DealerSkuMapping> mappings = List.of(
                mapping(dealerId, "WPW-001", "D-100"),
                mapping(dealerId, "WPW-002", "D-200")
            );
            when(mappingRepo.findByDealerIdAndWpwSkuIn(eq(dealerId), any(Collection.class)))
                .thenReturn(mappings);

            Map<String, String> result = service.resolveBatch(List.of("WPW-001", "WPW-002", "WPW-003"), auth);

            assertThat(result).containsEntry("WPW-001", "D-100")
                              .containsEntry("WPW-002", "D-200")
                              .doesNotContainKey("WPW-003");
        }

        @Test
        @DisplayName("returns empty map for administrator")
        void returnsEmptyForAdmin() {
            Map<String, String> result = service.resolveBatch(List.of("WPW-001"), authAsAdmin());
            assertThat(result).isEmpty();
            verifyNoInteractions(mappingRepo);
        }

        @Test
        @DisplayName("returns empty map when auth is null")
        void returnsEmptyForNullAuth() {
            Map<String, String> result = service.resolveBatch(List.of("WPW-001"), null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty map with empty toolNos list")
        void returnsEmptyForEmptyToolNos() {
            Map<String, String> result = service.resolveBatch(List.of(), authAsDealer(dealerId));
            assertThat(result).isEmpty();
            verifyNoInteractions(mappingRepo);
        }

        @Test
        @DisplayName("returns empty map if no mappings")
        void returnsEmptyWhenNoMappings() {
            Authentication auth = authAsDealer(dealerId);
            when(mappingRepo.findByDealerIdAndWpwSkuIn(eq(dealerId), any(Collection.class)))
                .thenReturn(List.of());

            Map<String, String> result = service.resolveBatch(List.of("WPW-999"), auth);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolve (single)")
    class ResolveSingle {

        @Test
        @DisplayName("returns dealerSku if mapping exists")
        void returnsDealerSkuWhenMappingExists() {
            Authentication auth = authAsDealer(dealerId);
            DealerSkuMappingId id = new DealerSkuMappingId(dealerId, "WPW-001");
            when(mappingRepo.findById(id)).thenReturn(Optional.of(mapping(dealerId, "WPW-001", "D-100")));

            String result = service.resolve("WPW-001", auth);

            assertThat(result).isEqualTo("D-100");
        }

        @Test
        @DisplayName("returns null if no mapping")
        void returnsNullWhenNoMapping() {
            Authentication auth = authAsDealer(dealerId);
            when(mappingRepo.findById(any())).thenReturn(Optional.empty());

            String result = service.resolve("WPW-999", auth);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null for administrator")
        void returnsNullForAdmin() {
            String result = service.resolve("WPW-001", authAsAdmin());
            assertThat(result).isNull();
            verifyNoInteractions(mappingRepo);
        }

        @Test
        @DisplayName("returns null when auth is null")
        void returnsNullForNullAuth() {
            String result = service.resolve("WPW-001", null);
            assertThat(result).isNull();
        }
    }
}
