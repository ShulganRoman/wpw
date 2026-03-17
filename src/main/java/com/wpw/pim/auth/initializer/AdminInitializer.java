package com.wpw.pim.auth.initializer;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.domain.User;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_ROLE_NAME = "admin";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByName(ADMIN_ROLE_NAME)
                .orElseGet(() -> {
                    log.info("Creating built-in admin role with all privileges");
                    Role role = new Role(ADMIN_ROLE_NAME, true, EnumSet.allOf(Privilege.class));
                    return roleRepository.save(role);
                });

        // Ensure admin role always has all privileges
        if (!adminRole.getPrivileges().containsAll(EnumSet.allOf(Privilege.class))) {
            log.info("Updating admin role to include all privileges");
            adminRole.setPrivileges(EnumSet.allOf(Privilege.class));
            roleRepository.save(adminRole);
        }

        if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("Creating default admin user");
            User admin = new User(
                    ADMIN_USERNAME,
                    passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD),
                    adminRole
            );
            userRepository.save(admin);
        }
    }
}
