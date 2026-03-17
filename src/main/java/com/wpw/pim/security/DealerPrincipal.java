package com.wpw.pim.security;

import com.wpw.pim.domain.dealer.Dealer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class DealerPrincipal implements UserDetails {

    private final Dealer dealer;

    public DealerPrincipal(Dealer dealer) {
        this.dealer = dealer;
    }

    public Dealer getDealer() {
        return dealer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_DEALER"));
    }

    @Override public String getPassword() { return dealer.getApiKeyHash(); }
    @Override public String getUsername() { return dealer.getName(); }
    @Override public boolean isEnabled() { return dealer.isActive(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
