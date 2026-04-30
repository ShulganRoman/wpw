package com.wpw.pim.auth.filter;

import com.wpw.pim.auth.service.JwtService;
import com.wpw.pim.auth.service.PimUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для {@link JwtAuthFilter}.
 * Покрывают пути: отсутствие Bearer-токена, валидный токен, невалидный токен,
 * отключённый юзер, уже установленная аутентификация.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private PimUserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter filter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("без заголовка Authorization — пропускает фильтр-цепочку без аутентификации")
    void missingHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, filterChain);

        verify(filterChain).doFilter(req, resp);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("заголовок без 'Bearer ' префикса — пропускает фильтр")
    void wrongPrefix() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, filterChain);

        verify(filterChain).doFilter(req, resp);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("валидный Bearer токен и enabled пользователь — устанавливает аутентификацию")
    void validToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        UserDetails ud = User.withUsername("alice").password("p")
            .authorities(new SimpleGrantedAuthority("MANAGE_CATALOG"))
            .build();

        when(jwtService.extractUsername("valid-token")).thenReturn(Optional.of("alice"));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(ud);

        filter.doFilter(req, resp, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        verify(filterChain).doFilter(req, resp);
    }

    @Test
    @DisplayName("невалидный токен (extractUsername возвращает empty) — пропускает фильтр")
    void invalidToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.extractUsername("broken")).thenReturn(Optional.empty());

        filter.doFilter(req, resp, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(req, resp);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("disabled пользователь — аутентификация не устанавливается")
    void disabledUser() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        UserDetails ud = User.withUsername("blocked").password("p")
            .disabled(true)
            .authorities(new SimpleGrantedAuthority("USER"))
            .build();

        when(jwtService.extractUsername("valid-token")).thenReturn(Optional.of("blocked"));
        when(userDetailsService.loadUserByUsername("blocked")).thenReturn(ud);

        filter.doFilter(req, resp, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(req, resp);
    }

    @Test
    @DisplayName("уже установленная аутентификация (например, от API key filter) — не перезаписывается")
    void existingAuthIsKept() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        Authentication preset = new UsernamePasswordAuthenticationToken("apiUser", null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_DEALER")));
        SecurityContextHolder.getContext().setAuthentication(preset);

        when(jwtService.extractUsername("valid-token")).thenReturn(Optional.of("alice"));

        filter.doFilter(req, resp, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(preset);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(req, resp);
    }
}
