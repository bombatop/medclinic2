package com.medclinic.auth.service;

import com.medclinic.auth.exception.ResourceNotFoundException;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.RoleRepository;
import com.medclinic.auth.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public Set<Role> resolveRoles(User user) {
        Set<Role> roles = normalizeRoles(user.getEffectiveRoles());
        if (roles.isEmpty()) {
            roleRepository.findByCode(ROLE_RECEPTIONIST).ifPresent(roles::add);
        }
        return roles;
    }

    public Set<Role> normalizeRoles(Collection<Role> roles) {
        return roles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        role -> role.getCode().toUpperCase(Locale.ROOT),
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(role -> role.getCode().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> normalizeRoleCodes(Collection<String> roleCodes) {
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public Set<Role> resolveRoleEntitiesByCodes(Collection<String> roleCodes) {
        Set<String> normalizedCodes = normalizeRoleCodes(roleCodes);
        if (normalizedCodes.isEmpty()) {
            return Set.of();
        }

        List<Role> found = roleRepository.findByCodeIn(normalizedCodes);
        Map<String, Role> byCode = found.stream()
                .collect(Collectors.toMap(role -> role.getCode().toUpperCase(Locale.ROOT), role -> role));

        List<String> missing = normalizedCodes.stream()
                .filter(code -> !byCode.containsKey(code))
                .toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Roles not found: " + String.join(", ", missing));
        }

        return normalizeRoles(byCode.values());
    }

    @Transactional(readOnly = true)
    public Set<String> resolvePermissionCodes(Collection<Role> roles) {
        if (roles.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.findByRoleIn(roles).stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(permission -> permission.getCode())
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<String> toRoleCodes(Collection<Role> roles) {
        return normalizeRoles(roles).stream()
                .map(Role::getCode)
                .toList();
    }

    public String joinRoles(Collection<Role> roles) {
        return normalizeRoles(roles).stream()
                .map(Role::getCode)
                .collect(Collectors.joining(","));
    }
}
