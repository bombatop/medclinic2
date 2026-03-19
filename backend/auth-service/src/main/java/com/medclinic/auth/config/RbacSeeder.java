package com.medclinic.auth.config;

import com.medclinic.auth.model.Permission;
import com.medclinic.auth.model.PermissionEntity;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import com.medclinic.auth.repository.PermissionEntityRepository;
import com.medclinic.auth.repository.RoleRepository;
import com.medclinic.auth.repository.RolePermissionRepository;
import com.medclinic.auth.repository.UserRepository;
import com.medclinic.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class RbacSeeder implements ApplicationRunner {

    private final PermissionEntityRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RbacService rbacService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedRolePermissions();
        backfillUserRoles();
    }

    private void seedPermissions() {
        Set<String> existingCodes = permissionRepository.findAll().stream()
                .map(permission -> permission.getCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<PermissionEntity> toCreate = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            String code = permission.code().toLowerCase(Locale.ROOT);
            if (!existingCodes.contains(code)) {
                toCreate.add(PermissionEntity.builder()
                        .code(code)
                        .name(titleCaseCode(permission.code()))
                        .description("Business action permission: " + permission.code())
                        .active(true)
                        .system(true)
                        .build());
            }
        }

        if (!toCreate.isEmpty()) {
            permissionRepository.saveAll(toCreate);
            log.info("RBAC seed inserted {} permissions", toCreate.size());
        }
    }

    private void seedRoles() {
        List<Role> defaults = List.of(
                Role.builder()
                        .code(RbacService.ROLE_ADMIN)
                        .name("Administrator")
                        .description("System administrator role with broad access.")
                        .active(true)
                        .system(true)
                        .build(),
                Role.builder()
                        .code(RbacService.ROLE_DOCTOR)
                        .name("Doctor")
                        .description("Doctor role with appointment participation permissions.")
                        .active(true)
                        .system(true)
                        .build(),
                Role.builder()
                        .code(RbacService.ROLE_RECEPTIONIST)
                        .name("Receptionist")
                        .description("Receptionist role with read-only appointment and employee access.")
                        .active(true)
                        .system(true)
                        .build()
        );

        Set<String> existingCodes = roleRepository.findAll().stream()
                .map(role -> role.getCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<Role> toCreate = defaults.stream()
                .filter(role -> !existingCodes.contains(role.getCode()))
                .toList();

        if (!toCreate.isEmpty()) {
            roleRepository.saveAll(toCreate);
            log.info("RBAC seed inserted {} baseline roles", toCreate.size());
        }
    }

    private void seedRolePermissions() {
        Map<String, Set<String>> mapping = rolePermissionMapping();
        Map<String, Role> rolesByCode = roleRepository.findAll().stream()
                .collect(Collectors.toMap(role -> role.getCode().toUpperCase(Locale.ROOT), Function.identity()));
        Map<String, PermissionEntity> permissionsByCode = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(permission -> permission.getCode().toLowerCase(Locale.ROOT), Function.identity()));

        Set<String> existing = rolePermissionRepository.findAll().stream()
                .map(entry -> key(entry.getRole(), entry.getPermission()))
                .collect(java.util.stream.Collectors.toSet());

        List<RolePermission> toCreate = new ArrayList<>();
        mapping.forEach((roleCode, permissionCodes) -> permissionCodes.forEach(permissionCode -> {
            Role role = rolesByCode.get(roleCode);
            PermissionEntity permission = permissionsByCode.get(permissionCode);
            if (role == null || permission == null) {
                return;
            }
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

    private static String key(Role role, PermissionEntity permission) {
        return role.getCode().toUpperCase(Locale.ROOT) + "|" + permission.getCode().toUpperCase(Locale.ROOT);
    }

    private Map<String, Set<String>> rolePermissionMapping() {
        return Map.of(
                RbacService.ROLE_ADMIN, Set.of(
                        Permission.USERS_READ_ALL.code().toLowerCase(Locale.ROOT),
                        Permission.USERS_MANAGE.code().toLowerCase(Locale.ROOT),
                        Permission.USERS_MANAGE_ROLES.code().toLowerCase(Locale.ROOT),
                        Permission.EMPLOYEE_READ_ALL.code().toLowerCase(Locale.ROOT),
                        Permission.EMPLOYEE_MANAGE.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_READ_ALL.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CREATE_ANY.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_UPDATE_ANY.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_STATUS_UPDATE_ANY.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CANCEL_ANY.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CREATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_UPDATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_STATUS_UPDATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CANCEL_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_PARTICIPATE.code().toLowerCase(Locale.ROOT)
                ),
                RbacService.ROLE_DOCTOR, Set.of(
                        Permission.APPOINTMENT_READ_ALL.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CREATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_UPDATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_STATUS_UPDATE_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_CANCEL_SELF.code().toLowerCase(Locale.ROOT),
                        Permission.APPOINTMENT_PARTICIPATE.code().toLowerCase(Locale.ROOT),
                        Permission.EMPLOYEE_READ_ALL.code().toLowerCase(Locale.ROOT)
                ),
                RbacService.ROLE_RECEPTIONIST, Set.of(
                        Permission.APPOINTMENT_READ_ALL.code().toLowerCase(Locale.ROOT),
                        Permission.EMPLOYEE_READ_ALL.code().toLowerCase(Locale.ROOT)
        ));
    }

    private static String titleCaseCode(String code) {
        return java.util.Arrays.stream(code.split("[._]"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }
}
