package com.wpw.pim.service.dealer;

import com.wpw.pim.domain.dealer.DealerSkuMapping;
import com.wpw.pim.domain.dealer.DealerSkuMappingId;
import com.wpw.pim.repository.dealer.DealerRepository;
import com.wpw.pim.repository.dealer.DealerSkuMappingRepository;
import com.wpw.pim.security.DealerPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DealerSkuResolverService {

    private final DealerSkuMappingRepository mappingRepo;
    private final DealerRepository dealerRepo;

    @Transactional(readOnly = true)
    public Map<String, String> resolveBatch(Collection<String> toolNos, Authentication auth) {
        UUID dealerId = extractDealerId(auth);
        if (dealerId == null || toolNos.isEmpty()) return Collections.emptyMap();

        return mappingRepo.findByDealerIdAndWpwSkuIn(dealerId, toolNos).stream()
            .collect(Collectors.toMap(
                m -> m.getId().getWpwSku(),
                DealerSkuMapping::getDealerSku
            ));
    }

    @Transactional(readOnly = true)
    public String resolve(String toolNo, Authentication auth) {
        UUID dealerId = extractDealerId(auth);
        if (dealerId == null) return null;

        return mappingRepo.findById(new DealerSkuMappingId(dealerId, toolNo))
            .map(DealerSkuMapping::getDealerSku)
            .orElse(null);
    }

    private UUID extractDealerId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof DealerPrincipal dp) {
            return dp.getDealer().getId();
        }

        if (principal instanceof UserDetails ud && hasRole(auth, "dealer")) {
            return dealerRepo.findByUserUsername(ud.getUsername())
                .map(d -> d.getId())
                .orElse(null);
        }

        return null;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equalsIgnoreCase("ROLE_" + role));
    }
}
