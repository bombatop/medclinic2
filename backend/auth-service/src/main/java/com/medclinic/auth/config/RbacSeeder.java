package com.medclinic.auth.config;

import com.medclinic.auth.model.Permission;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import com.medclinic.auth.repository.RolePermissionRepository;
import com.medclinic.auth.repository.UserRepository;
import com.medclinic.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RbacSeeder implements ApplicationRunner {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRolePermissions();
        backfillUserRoles();
    }

    private void seedRolePermissions() {
        Map<Role, Set<Permission>> mapping = rolePermissionMapping();
        Set<String> existing = rolePermissionRepository.findAll().stream()
                .map(entry -> key(entry.getRole(), entry.getPermission()))
                .collect(java.util.stream.Collectors.toSet());

        List<RolePermission> toCreate = new ArrayList<>();
        mapping.forEach((role, permissions) -> permissions.forEach(permission -> {
            if (!existing.contains(key(role, permission))) {
                toCreate.add(RolePermission.builder().role(role).permission(permission).build());
            }
        }));

        if (!toCreate.isEmpty()) {
            rolePermissionRepository.saveAll(toCreate);
            log.info("RBAC seed inserted {} role-permission entries", toCreate.size());
        }
    }

    private void backfillUserRoles() {
        var users = userRepository.findAll();
        int updated = 0;
        for (var user : users) {
            var normalized = rbacService.resolveRoles(user);
            var currentRoles = user.getRoles() == null ? Set.<Role>of() : user.getRoles();
            if (!normalized.equals(currentRoles)) {
                user.setRoles(new LinkedHashSet<>(normalized));
                updated++;
            }
        }
        if (updated > 0) {
            userRepository.saveAll(users);
            log.info("RBAC backfilled roles for {} users", updated);
        }
    }

    private static String key(Role role, Permission permission) {
        return role.name() + "|" + permission.name();
    }

    private Map<Role, Set<Permission>> rolePermissionMapping() {
        Map<Role, Set<Permission>> mapping = new EnumMap<>(Role.class);

        mapping.put(Role.ADMIN, Set.of(
                Permission.USERS_READ_ALL,
                Permission.USERS_MANAGE,
                Permission.USERS_MANAGE_ROLES,
                Permission.EMPLOYEE_READ_ALL,
                Permission.EMPLOYEE_MANAGE,
                Permission.APPOINTMENT_READ_ALL,
                Permission.APPOINTMENT_CREATE_ANY,
                Permission.APPOINTMENT_UPDATE_ANY,
                Permission.APPOINTMENT_STATUS_UPDATE_ANY,
                Permission.APPOINTMENT_CANCEL_ANY,
                Permission.APPOINTMENT_CREATE_SELF,
                Permission.APPOINTMENT_UPDATE_SELF,
                Permission.APPOINTMENT_STATUS_UPDATE_SELF,
                Permission.APPOINTMENT_CANCEL_SELF,
                Permission.APPOINTMENT_PARTICIPATE
        ));

        mapping.put(Role.DOCTOR, Set.of(
                Permission.APPOINTMENT_READ_ALL,
                Permission.APPOINTMENT_CREATE_SELF,
                Permission.APPOINTMENT_UPDATE_SELF,
                Permission.APPOINTMENT_STATUS_UPDATE_SELF,
                Permission.APPOINTMENT_CANCEL_SELF,
                Permission.APPOINTMENT_PARTICIPATE,
                Permission.EMPLOYEE_READ_ALL
        ));

        mapping.put(Role.RECEPTIONIST, Set.of(
                Permission.APPOINTMENT_READ_ALL,
                Permission.EMPLOYEE_READ_ALL
        ));

        return mapping;
    }
}
