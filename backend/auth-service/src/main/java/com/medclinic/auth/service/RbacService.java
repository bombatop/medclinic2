package com.medclinic.auth.service;

import com.medclinic.auth.model.Permission;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RbacService {

    private static final List<Role> ROLE_PRIORITY = List.of(Role.ADMIN, Role.DOCTOR, Role.RECEPTIONIST);

    private final RolePermissionRepository rolePermissionRepository;

    public Set<Role> resolveRoles(User user) {
        Set<Role> roles = user.getEffectiveRoles();
        if (roles.isEmpty()) {
            roles.add(Role.RECEPTIONIST);
        }
        return normalizeRoles(roles);
    }

    public Set<Role> normalizeRoles(Collection<Role> roles) {
        LinkedHashSet<Role> normalized = new LinkedHashSet<>();
        for (Role role : ROLE_PRIORITY) {
            if (roles.contains(role)) {
                normalized.add(role);
            }
        }
        for (Role role : roles) {
            if (!normalized.contains(role)) {
                normalized.add(role);
            }
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public Set<Permission> resolvePermissions(Collection<Role> roles) {
        if (roles.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.findByRoleIn(roles).stream()
                .map(RolePermission::getPermission)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public List<String> toRoleCodes(Collection<Role> roles) {
        return normalizeRoles(roles).stream().map(Role::name).toList();
    }

    public List<String> toPermissionCodes(Collection<Permission> permissions) {
        return permissions.stream()
                .map(Permission::code)
                .sorted()
                .toList();
    }

    public String joinRoles(Collection<Role> roles) {
        return normalizeRoles(roles).stream()
                .map(Role::name)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
