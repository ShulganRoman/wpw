package com.wpw.pim.auth.service;

import com.wpw.pim.auth.domain.Privilege;
import com.wpw.pim.auth.domain.Role;
import com.wpw.pim.auth.dto.RoleRequest;
import com.wpw.pim.auth.dto.RoleResponse;
import com.wpw.pim.auth.repository.RoleRepository;
import com.wpw.pim.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return roleRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }

        Set<Privilege> privileges = parsePrivileges(request.privileges());
        Role role = new Role(request.name(), false, privileges);
        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

        // Check name uniqueness if changed
        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }

        Set<Privilege> privileges = parsePrivileges(request.privileges());
        role.setName(request.name());
        role.setPrivileges(privileges);
        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

        if (role.isBuiltIn()) {
            throw new IllegalArgumentException("Cannot delete built-in role '" + role.getName() + "'");
        }

        if (userRepository.existsByRoleId(id)) {
            throw new IllegalArgumentException("Cannot delete role '" + role.getName() + "': it is assigned to users");
        }

        roleRepository.delete(role);
    }

    private Set<Privilege> parsePrivileges(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return EnumSet.noneOf(Privilege.class);
        }
        return names.stream()
                .map(name -> {
                    try {
                        return Privilege.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Unknown privilege: " + name);
                    }
                })
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Privilege.class)));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.isBuiltIn(),
                role.getPrivileges().stream().map(Enum::name).collect(Collectors.toSet()),
                role.getCreatedAt()
        );
    }
}
