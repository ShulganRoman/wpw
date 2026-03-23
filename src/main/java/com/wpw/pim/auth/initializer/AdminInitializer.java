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
import java.util.Set;

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
        // --- admin role: all privileges ---
        Role adminRole = roleRepository.findByName(ADMIN_ROLE_NAME)
                .orElseGet(() -> {
                    log.info("Creating built-in admin role with all privileges");
                    return roleRepository.save(new Role(ADMIN_ROLE_NAME, true, EnumSet.allOf(Privilege.class)));
                });

        if (!adminRole.getPrivileges().containsAll(EnumSet.allOf(Privilege.class))) {
            log.info("Updating admin role to include all privileges");
            adminRole.setPrivileges(EnumSet.allOf(Privilege.class));
            roleRepository.save(adminRole);
        }

        // --- dealer role: modify products ---
        ensureRole("dealer", Set.of(Privilege.MODIFY_PRODUCTS));

        // --- user role: export only ---
        ensureRole("user", Set.of(Privilege.BULK_EXPORT));

        // --- default admin user ---
        if (!userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("Creating default admin user");
            userRepository.save(new User(
                    ADMIN_USERNAME,
                    passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD),
                    adminRole
            ));
        }
    }

    private void ensureRole(String name, Set<Privilege> privileges) {
        roleRepository.findByName(name).orElseGet(() -> {
            log.info("Creating built-in role '{}' with privileges {}", name, privileges);
            return roleRepository.save(new Role(name, true, EnumSet.copyOf(privileges)));
        });
    }
}
