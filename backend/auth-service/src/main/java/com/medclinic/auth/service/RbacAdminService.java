package com.medclinic.auth.service;

import com.medclinic.auth.dto.*;
import com.medclinic.auth.exception.ConflictException;
import com.medclinic.auth.exception.ResourceNotFoundException;
import com.medclinic.auth.model.PermissionEntity;
import com.medclinic.auth.model.RbacAuditLog;
import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import com.medclinic.auth.model.User;
import com.medclinic.auth.repository.PermissionEntityRepository;
import com.medclinic.auth.repository.RbacAuditLogRepository;
import com.medclinic.auth.repository.RolePermissionRepository;
import com.medclinic.auth.repository.RoleRepository;
import com.medclinic.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacAdminService {

    private static final String TARGET_ROLE = "ROLE";
    private static final String TARGET_ROLE_PERMISSION = "ROLE_PERMISSION";

    private final RoleRepository roleRepository;
    private final PermissionEntityRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final RbacAuditLogRepository rbacAuditLogRepository;

    @Transactional(readOnly = true)
    public Page<RoleResponse> getRoles(String search, Pageable pageable) {
        Specification<Role> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            );
        };
        return roleRepository.findAll(spec, pageable).map(RoleResponse::from);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long roleId) {
        return RoleResponse.from(findRole(roleId));
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String actorUsername, Long actorUserId) {
        String code = normalizeCode(request.code());
        if (roleRepository.existsByCode(code)) {
            throw new ConflictException("Role already exists: " + code);
        }

        Role role = Role.builder()
                .code(code)
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .active(request.active() == null || request.active())
                .system(false)
                .build();
        Role created = roleRepository.save(role);
        logAudit(actorUsername, actorUserId, "ROLE_CREATED", TARGET_ROLE, created.getCode(),
                "Created role code=" + created.getCode());
        return RoleResponse.from(created);
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, UpdateRoleRequest request, String actorUsername, Long actorUserId) {
        Role role = findRole(roleId);
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        if (request.active() != null) {
            role.setActive(request.active());
        }
        Role saved = roleRepository.save(role);
        logAudit(actorUsername, actorUserId, "ROLE_UPDATED", TARGET_ROLE, saved.getCode(),
                "Updated role metadata for code=" + saved.getCode());
        return RoleResponse.from(saved);
    }

    @Transactional
    public void deleteRole(Long roleId, String actorUsername, Long actorUserId) {
        Role role = findRole(roleId);
        if (role.isSystem()) {
            throw new ConflictException("Cannot delete system role: " + role.getCode());
        }
        List<User> usersWithRole = userRepository.findByRolesId(roleId);
        for (User user : usersWithRole) {
            user.getRoles().removeIf(existingRole -> Objects.equals(existingRole.getId(), roleId));
        }
        if (!usersWithRole.isEmpty()) {
            userRepository.saveAll(usersWithRole);
        }
        rolePermissionRepository.deleteByRole(role);
        roleRepository.delete(role);
        logAudit(actorUsername, actorUserId, "ROLE_DELETED", TARGET_ROLE, role.getCode(),
                "Deleted role code=" + role.getCode() + "; detached from users=" + usersWithRole.size());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(permission -> permission.getCode().toUpperCase(Locale.ROOT)))
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolePermissionsResponse getRolePermissions(Long roleId) {
        Role role = findRole(roleId);
        List<String> permissionCodes = rolePermissionRepository.findByRole(role).stream()
                .map(RolePermission::getPermission)
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();
        return new RolePermissionsResponse(role.getId(), role.getCode(), permissionCodes);
    }

    @Transactional
    public RolePermissionsResponse updateRolePermissions(Long roleId,
                                                         UpdateRolePermissionsRequest request,
                                                         String actorUsername,
                                                         Long actorUserId) {
        Role role = findRole(roleId);
        Set<String> requestedCodes = request.permissions() == null
                ? Set.of()
                : request.permissions().stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<PermissionEntity> permissions = requestedCodes.isEmpty()
                ? List.of()
                : permissionRepository.findByCodeIn(requestedCodes);
        Set<String> foundCodes = permissions.stream()
                .map(permission -> permission.getCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> missingCodes = requestedCodes.stream()
                .filter(code -> !foundCodes.contains(code))
                .sorted()
                .toList();
        if (!missingCodes.isEmpty()) {
            throw new ResourceNotFoundException("Permissions not found: " + String.join(", ", missingCodes));
        }

        List<String> beforeCodes = rolePermissionRepository.findByRole(role).stream()
                .map(RolePermission::getPermission)
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();

        rolePermissionRepository.deleteByRole(role);
        if (!permissions.isEmpty()) {
            List<RolePermission> mappings = permissions.stream()
                    .map(permission -> RolePermission.builder().role(role).permission(permission).build())
                    .toList();
            rolePermissionRepository.saveAll(mappings);
        }

        List<String> afterCodes = requestedCodes.stream().sorted().toList();
        logAudit(actorUsername, actorUserId, "ROLE_PERMISSIONS_UPDATED", TARGET_ROLE_PERMISSION, role.getCode(),
                "before=" + String.join(",", beforeCodes) + ";after=" + String.join(",", afterCodes));

        return new RolePermissionsResponse(role.getId(), role.getCode(), afterCodes);
    }

    @Transactional(readOnly = true)
    public Page<RbacAuditLogResponse> getAuditLogs(String actorUsername,
                                                   String action,
                                                   Instant fromTs,
                                                   Instant toTs,
                                                   Pageable pageable) {
        String normalizedActor = trimToNull(actorUsername);
        String normalizedAction = trimToNull(action);
        if (normalizedActor == null && normalizedAction == null && fromTs == null && toTs == null) {
            return rbacAuditLogRepository.findAll(pageable).map(RbacAuditLogResponse::from);
        }

        List<RbacAuditLogResponse> filtered = rbacAuditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .filter(log -> normalizedActor == null || log.getActorUsername().equals(normalizedActor))
                .filter(log -> normalizedAction == null || log.getAction().equals(normalizedAction))
                .filter(log -> fromTs == null || !log.getCreatedAt().isBefore(fromTs))
                .filter(log -> toTs == null || !log.getCreatedAt().isAfter(toTs))
                .map(RbacAuditLogResponse::from)
                .toList();

        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional
    public void logAudit(String actorUsername,
                         Long actorUserId,
                         String action,
                         String targetType,
                         String targetRef,
                         String details) {
        RbacAuditLog log = RbacAuditLog.builder()
                .actorUsername(actorUsername == null ? "system" : actorUsername)
                .actorUserId(resolveActorUserId(actorUsername, actorUserId))
                .action(action)
                .targetType(targetType)
                .targetRef(targetRef)
                .details(details == null ? "" : details)
                .build();
        rbacAuditLogRepository.save(log);
    }

    private Long resolveActorUserId(String actorUsername, Long actorUserId) {
        if (actorUserId != null) {
            return actorUserId;
        }
        if (actorUsername == null || actorUsername.isBlank()) {
            return 0L;
        }
        return userRepository.findByUsername(actorUsername).map(User::getId).orElse(0L);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ConflictException("Role code is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
