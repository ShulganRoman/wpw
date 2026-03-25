package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.dto.UserRequest;
import com.wpw.pim.auth.dto.UserResponse;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("User with username '" + request.username() + "' already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + request.roleId()));

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                role
        );
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Check username uniqueness if changed
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("User with username '" + request.username() + "' already exists");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + request.roleId()));

        user.setUsername(request.username());
        user.setRole(role);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        userRepository.delete(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().getId(),
                user.getRole().getName(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
