package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.dto.LoginRequest;
import com.wpw.pim.auth.dto.LoginResponse;
import com.wpw.pim.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameWithRole(request.username())
                .filter(u -> u.isEnabled() && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String token = jwtService.generateToken(user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole().getName(),
                user.getRole().getPrivileges().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
    }
}
